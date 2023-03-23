#pragma once

#include <errno.h>
#include "pthread.h"
#include <string.h>
#include <stdlib.h>

#include "Tree.h"
#include "Node.h"

// if inside move function source path is a prefix of target path, it would
// try to connect target's (in)direct parent as target's son, making that part
// of the tree unreachable. We define the following error code for this case.
#define EBADMOVE -1

struct Tree {
    Node* root;
};


Tree* tree_new() {
    Tree* tree = safe_malloc(sizeof(Tree));

    tree->root = node_new(NULL, "");

    return tree;
}

void tree_free(Tree* tree) {
    node_free(tree->root);
    free(tree);
}

int process_path(Node** node, const char* path, bool notify_path_root, bool skip_root_node_protocols) {
    char* component = safe_malloc(MAX_FOLDER_NAME_LENGTH + 1);
    const char* new_path = path;
    Node* last_to_notify = notify_path_root ? (*node)->parent : *node;
    new_path = split_path(new_path, component);

    while (new_path) {
        if (skip_root_node_protocols == false && node_reader_preliminary_protocol(*node)) {
            node_reader_final_protocol(*node);
            free(component);
            notify_parent_nodes(*node, last_to_notify);
            return ENOENT;
        }

        Node* next_node = safe_hmap_get((*node)->children, component);

        if (skip_root_node_protocols) {
            skip_root_node_protocols = false;
        }
        else {
            node_reader_final_protocol(*node);
        }

        if (!next_node) {
            break;
        }

        *node = next_node;
        new_path = split_path(new_path, component);
    }
    free(component);

    if (new_path) {
        notify_parent_nodes(*node, last_to_notify);
        return ENOENT;
    }

    return 0;
}

char* tree_list(Tree* tree, const char* path) {
    if (!is_path_valid(path)) {
        return NULL;
    }

    Node* node = tree->root;
    int process_path_return_code = process_path(
            &node,
            path,
            true,
            false);

    if (process_path_return_code) {
        return NULL;
    }

    if (node_reader_preliminary_protocol(node)) {
        node_reader_final_protocol(node);
        notify_parent_nodes(node, NULL);
        return NULL;
    }

    char* result = safe_make_map_contents_string(node->children);

    node_reader_final_protocol(node);
    notify_parent_nodes(node, NULL);

    return result;
}

int tree_create(Tree* tree, const char* path) {
    if (!is_path_valid(path)) {
        return EINVAL;
    }
    if (strcmp(path, "/") == 0) {
        return EEXIST;
    }

    Node* node = tree->root;
    char* target = safe_malloc(MAX_FOLDER_NAME_LENGTH + 1);
    char* path_to_parent = make_path_to_parent(path, target);

    int process_path_return_code = process_path(
            &node,
            path_to_parent,
            true,
            false);

    free(path_to_parent);

    if (process_path_return_code) {
        free(target);
        return process_path_return_code;
    }

    Node* target_node = node_new(node, target);

    if (node_reader_preliminary_protocol(node)) {
        node_reader_final_protocol(node);
        notify_parent_nodes(node, NULL);
        free(target);
        node_free(target_node);
        return ENOENT;
    }

    bool insert_success = safe_hmap_insert(node->children, target, target_node);

    node_reader_final_protocol(node);
    free(target);
    notify_parent_nodes(node, NULL);

    if (!insert_success) {
        node_free(target_node);
        return EEXIST;
    }

    return 0;
}

int tree_remove(Tree* tree, const char* path) {
    if (!is_path_valid(path)) {
        return EINVAL;
    }
    if (strcmp(path, "/") == 0) {
        return EBUSY;
    }

    Node* node = tree->root;
    int process_path_return_code = process_path(
            &node,
            path,
            true,
            false);

    if (process_path_return_code) {
        return process_path_return_code;
    }

    if (node_writer_preliminary_protocol(node)) {
        node_writer_final_protocol(node);
        notify_parent_nodes(node, NULL);
        return ENOENT;
    }

    if (safe_hmap_size(node->children)) {
        node_writer_final_protocol(node);
        notify_parent_nodes(node, NULL);
        return ENOTEMPTY;
    }

    safe_hmap_remove(node->parent->children, node->name);

    node_discard_waiting(node);

    node_writer_final_protocol(node);
    notify_parent_nodes(node, NULL);
    node_free(node);

    return 0;
}

int tree_move(Tree* tree, const char* source, const char* target) {
    if (!is_path_valid(source) || !is_path_valid(target)) {
        return EINVAL;
    }
    if (strcmp(source, "/") == 0) {
        return EBUSY;
    }
    if (strcmp(target, "/") == 0) {
        return EEXIST;
    }
    if (strncmp(source, target, strlen(source)) == 0 &&
        strcmp(source, target) != 0) {
        return EBADMOVE;
    }

    Node *shared_node = tree->root;
    char *target_name = safe_malloc(MAX_FOLDER_NAME_LENGTH + 1);
    char *path_to_target_parent = make_path_to_parent(target, target_name);

    char *shared_path = make_shared_path(source, path_to_target_parent);
    size_t shared_path_len = strlen(shared_path) - 1; // Exclude the end '/'.

    int process_path_return_code = process_path(
            &shared_node,
            shared_path,
            true,
            false);

    free(shared_path);

    if (process_path_return_code) {
        free(target_name);
        free(path_to_target_parent);
        return process_path_return_code;
    }

    Node *source_node = shared_node;
    Node *target_node_parent = shared_node;

    if (node_reader_preliminary_protocol(shared_node)) {
        free(target_name);
        free(path_to_target_parent);
        node_reader_final_protocol(shared_node);
        notify_parent_nodes(shared_node, NULL);
        return ENOENT;
    }

    if (compare_paths(source, target) <= 0) {

        // try to acquire the source node
        process_path_return_code = process_path(
                &source_node,
                source + shared_path_len,
                false,
                true);

        if (process_path_return_code) {
            free(target_name);
            free(path_to_target_parent);
            node_reader_final_protocol(shared_node);
            notify_parent_nodes(shared_node, NULL);
            return process_path_return_code;
        }

        if (node_writer_preliminary_protocol(source_node)) {
            free(target_name);
            free(path_to_target_parent);
            node_writer_final_protocol(source_node);
            node_reader_final_protocol(shared_node);
            notify_parent_nodes(source_node, NULL);
            return ENOENT;
        }

        if (strncmp(source, target, strlen(target)) == 0) {
            free(target_name);
            free(path_to_target_parent);
            node_writer_final_protocol(source_node);
            node_reader_final_protocol(shared_node);
            notify_parent_nodes(source_node, NULL);
            return EEXIST;
        }

        // try to acquire PARENT of target node
        process_path_return_code = process_path(
                &target_node_parent,
                path_to_target_parent + shared_path_len,
                false,
                true);

        free(path_to_target_parent);

        if (process_path_return_code) {
            free(target_name);
            node_writer_final_protocol(source_node);
            node_reader_final_protocol(shared_node);
            notify_parent_nodes(source_node, NULL);
            return process_path_return_code;
        }

        if (target_node_parent != shared_node &&
            node_reader_preliminary_protocol(target_node_parent)) {
            free(target_name);
            node_writer_final_protocol(source_node);
            node_reader_final_protocol(target_node_parent);
            node_reader_final_protocol(shared_node);
            notify_parent_nodes(source_node, NULL);
            notify_parent_nodes(target_node_parent, shared_node);
            return ENOENT;
        }
    } else {

        // try to acquire PARENT of target node
        process_path_return_code = process_path(
                &target_node_parent,
                path_to_target_parent + shared_path_len,
                false,
                true);

        free(path_to_target_parent);

        if (process_path_return_code) {
            free(target_name);
            node_reader_final_protocol(shared_node);
            notify_parent_nodes(shared_node, NULL);
            return process_path_return_code;
        }

        if (target_node_parent != shared_node &&
            node_reader_preliminary_protocol(target_node_parent)) {
            free(target_name);
            node_reader_final_protocol(target_node_parent);
            node_reader_final_protocol(shared_node);
            notify_parent_nodes(target_node_parent, NULL);
            return ENOENT;
        }

        // try to acquire the source node
        process_path_return_code = process_path(
                &source_node,
                source + shared_path_len,
                false,
                true);

        if (process_path_return_code) {
            free(target_name);
            if (target_node_parent != shared_node) {
                node_reader_final_protocol(target_node_parent);
            }
            node_reader_final_protocol(shared_node);
            notify_parent_nodes(target_node_parent, NULL);
            return process_path_return_code;
        }

        if (node_writer_preliminary_protocol(source_node)) {
            free(target_name);
            node_writer_final_protocol(source_node);
            if (target_node_parent != shared_node) {
                node_reader_final_protocol(target_node_parent);
            }
            node_reader_final_protocol(shared_node);
            notify_parent_nodes(source_node, NULL);
            notify_parent_nodes(target_node_parent, shared_node);
            return ENOENT;
        }
    }

    SafeHashMap *safe_map_left;
    SafeHashMap *safe_map_right;

    if (compare_paths(source, target) <= 0) {
        safe_map_left = source_node->parent->children;
        safe_map_right = target_node_parent->children;
    } else {
        safe_map_left = target_node_parent->children;
        safe_map_right = source_node->parent->children;
    }

    if (pthread_mutex_lock(&shared_node->move_map_writers_access)) {
        syserr("move_map_writers_access mutex_lock failed");
    }

    map_writer_preliminary_protocol(safe_map_left);

    if (safe_map_left != safe_map_right) {
        map_writer_preliminary_protocol(safe_map_right);
    }

    if (pthread_mutex_unlock(&shared_node->move_map_writers_access)) {
        syserr("move_map_writers_access mutex_unlock failed");
    }

    if (!hmap_insert(target_node_parent->children->map, target_name,
                     source_node)) {
        map_writer_final_protocol(safe_map_left);
        if (safe_map_left != safe_map_right) {
            map_writer_final_protocol(safe_map_right);
        }

        free(target_name);
        node_writer_final_protocol(source_node);
        if (target_node_parent != shared_node) {
            node_reader_final_protocol(target_node_parent);
        }
        node_reader_final_protocol(shared_node);
        notify_parent_nodes(source_node, NULL);
        notify_parent_nodes(target_node_parent, shared_node);

        return EEXIST;
    }

    hmap_remove(source_node->parent->children->map, source_node->name);

    node_discard_waiting(source_node);

    node_writer_final_protocol(source_node);
    notify_parent_nodes(source_node, NULL);

    strcpy(source_node->name, target_name);
    source_node->parent = target_node_parent;
    free(target_name);

    map_writer_final_protocol(safe_map_left);
    if (safe_map_left != safe_map_right) {
        map_writer_final_protocol(safe_map_right);
    }

    if (target_node_parent != shared_node) {
        node_reader_final_protocol(target_node_parent);
    }
    node_reader_final_protocol(shared_node);
    notify_parent_nodes(target_node_parent, shared_node);

    return 0;
}
