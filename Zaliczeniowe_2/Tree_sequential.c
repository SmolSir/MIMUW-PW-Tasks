#pragma once

#include <errno.h>
#include <stdlib.h>
#include <string.h>

#include "Tree.h"
#include "Node.h"
// #include "HashMap.h"
// #include "path_utils.h"

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

int process_path(Node** node, const char* path) {
    char* component = safe_malloc(MAX_FOLDER_NAME_LENGTH + 1);
    const char* new_path = path;
    new_path = split_path(new_path, component);

    while (new_path && *node) {
        // get map reader
        *node = hmap_get((*node)->children, component);
        // free map reader
        new_path = split_path(new_path, component);
    }
    free(component);

    if (new_path || !*node) {
        return ENOENT;
    }

    return 0;
}

char* tree_list(Tree* tree, const char* path) {
    if (!is_path_valid(path)) {
        return NULL;
    }

    Node* node = tree->root;
    int process_path_return_code = process_path(&node, path);
    if (process_path_return_code) {
        return NULL;
    }
    // get map reader
    return make_map_contents_string(node->children);
    // free map reader
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

    int process_path_return_code = process_path(&node, path_to_parent);
    free(path_to_parent);

    if (process_path_return_code) {
        free(target);
        return process_path_return_code;
    }

    Node* target_node = node_new(node, target);
    // get map writer
    bool insert_success = hmap_insert(node->children, target, target_node);
    // free map writer
    free(target);

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
    char* target = safe_malloc(MAX_FOLDER_NAME_LENGTH + 1);
    char* path_to_parent = make_path_to_parent(path, target);

    int process_path_return_code = process_path(&node, path_to_parent);
    free(path_to_parent);

    if (process_path_return_code) {
        free(target);
        return process_path_return_code;
    }

    // get map reader
    Node* child = hmap_get(node->children, target);
    // free map reader
    free(target);

    if (!child) {
        return ENOENT;
    }
    // get map reader
    size_t children_cnt = hmap_size(child->children);
    // free map reader
    if (children_cnt) {
        return ENOTEMPTY;
    }

    // get map writer
    hmap_remove(node->children, child->name);
    // free map writer
    node_free(child);

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

    Node* shared_node = tree->root;
    char* target_name = safe_malloc(MAX_FOLDER_NAME_LENGTH + 1);
    char* path_to_target_parent = make_path_to_parent(target, target_name);

    char* shared_path = make_shared_path(source, path_to_target_parent);
    size_t shared_path_len = strlen(shared_path) - 1; // Exclude the end '/'.

    int process_path_return_code = process_path(&shared_node, shared_path);
    free(shared_path);
    if (process_path_return_code) {
        free(target_name);
        free(path_to_target_parent);
        return process_path_return_code;
    }

    // try to acquire the source node
    Node* source_node = shared_node;
    process_path_return_code = process_path(&source_node, source + shared_path_len);
    if (process_path_return_code) {
        free(target_name);
        free(path_to_target_parent);
        return process_path_return_code;
    }

    // try to acquire PARENT of target node
    Node* target_node_parent = shared_node;
    process_path_return_code = process_path(&target_node_parent, path_to_target_parent + shared_path_len);
    free(path_to_target_parent);

    if (process_path_return_code) {
        free(target_name);
        return process_path_return_code;
    }

    // get map reader
    if (hmap_get(target_node_parent->children, target_name)) {
        // free map reader
        free(target_name);
        return EEXIST;
    }
    // free map reader

    if (strncmp(source, target, strlen(source)) == 0) {
        free(target_name);
        return EBADMOVE;
    }

    // remove from original parent's children, add to new parent's children,
    // update node's name and pointer to parent

    // get map writer
    hmap_remove(source_node->parent->children, source_node->name);
    // free map writer
    // get map writer
    hmap_insert(target_node_parent->children, target_name, source_node);
    // free map writer
    strcpy(source_node->name, target_name);
    source_node->parent = target_node_parent;
    free(target_name);

    return 0;
}