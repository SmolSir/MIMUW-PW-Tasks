#include <pthread.h>
#include <string.h>
#include <stdlib.h>

#include "Node.h"

Node* node_new(Node* parent, char* name) {
    Node* node = safe_malloc(sizeof(Node));

    node->parent = parent;
    node->name = safe_malloc(MAX_FOLDER_NAME_LENGTH + 1);
    strcpy(node->name, name);
    node->children = safe_hmap_new();

    if (pthread_mutex_init(&node->lock, 0)) {
        syserr("node lock init failed\n");
    }
    if (pthread_mutex_init(&node->thread_counter, 0)) {
        syserr("node thread counter init failed\n");
    }
    if (pthread_mutex_init(&node->move_map_writers_access, 0)) {
        syserr("node move_map_writers_access init failed\n");
    }

    if (pthread_cond_init(&node->readers, 0)) {
        syserr("node readers init failed\n");
    }
    if (pthread_cond_init(&node->writers, 0)) {
        syserr("node writers init failed\n");
    }
    if (pthread_cond_init(&node->private_access, 0)) {
        syserr("node private_access init failed\n");
    }

    node->readers_inside = 0;
    node->readers_waiting = 0;
    node->writers_inside = 0;
    node->writers_waiting = 0;
    node->subtree_threads = 0;
    node->safe_hmap_get_count = 0;

    node->change = WRITERS;
    node->emergency_pass = false;

    return node;
}

void node_free(Node* node) {
    const char* key;
    void* value;
    HashMapIterator it = hmap_iterator(node->children->map);
    while (hmap_next(node->children->map, &it, &key, &value)) {
        node_free(value);
    }

    if (pthread_mutex_destroy(&node->lock)) {
        syserr("node lock destroy failed\n");
    }
    if (pthread_mutex_destroy(&node->thread_counter)) {
        syserr("node thread counter destroy failed\n");
    }
    if (pthread_mutex_destroy(&node->move_map_writers_access)) {
        syserr("node move_map_writers_access destroy failed\n");
    }

    if (pthread_cond_destroy(&node->readers)) {
        syserr("node readers destroy failed\n");
    }
    if (pthread_cond_destroy(&node->writers)) {
        syserr("node writers destroy failed\n");
    }
    if (pthread_cond_destroy(&node->private_access)) {
        syserr("node private_access destroy failed\n");
    }

    safe_hmap_free(node->children);
    free(node->name);
    free(node);
}

void notify_parent_nodes(Node* first, Node* last) {
    while (first != last) {
        Node* node = first;
        first = first->parent;

        if (pthread_mutex_lock(&node->thread_counter)) {
            syserr("notify_parent_nodes mutex_lock failed\n");
        }

        node->subtree_threads--;
        if (node->parent) { // you cannot get root from safe_hmap_get
            node->safe_hmap_get_count--;
        }

        if (pthread_cond_signal(&node->private_access)) {
            syserr("notify_parent_nodes cond_signal failed\n");
        }

        if (pthread_mutex_unlock(&node->thread_counter)) {
            syserr("notify_parent_nodes mutex_unlock failed\n");
        }
    }
}

Node* safe_hmap_get(SafeHashMap* safe_map, const char* key) {
    map_reader_preliminary_protocol(safe_map);

    Node* node = hmap_get(safe_map->map, key);

    if (node) {
        if (pthread_mutex_lock(&node->thread_counter)) {
            syserr("safe_hmap_get mutex_lock failed");
        }

        node->safe_hmap_get_count++;

        if (pthread_mutex_unlock(&node->thread_counter)) {
            syserr("safe_hmap_get mutex_unlock failed");
        }
    }

    map_reader_final_protocol(safe_map);

    return node;
}

bool node_reader_preliminary_protocol(Node* node) {
    if (pthread_mutex_lock(&node->lock)) {
        syserr("node reader preliminary protocol mutex_lock failed\n");
    }

    while ((node->writers_inside > 0 ||
           (node->writers_waiting > 0 && node->change == WRITERS)) &&
            node->emergency_pass == false)
    {
        node->readers_waiting++;

        if (pthread_cond_wait(&node->readers, &node->lock)) {
            syserr("node reader preliminary protocol cond_wait failed\n");
        }

        node->readers_waiting--;
    }

    node->readers_inside++;

    if (!node->writers_waiting) {
        if (pthread_cond_signal(&node->readers)) {
            syserr("node reader preliminary protocol cond_signal failed\n");
        }
    }

    if (pthread_mutex_unlock(&node->lock)) {
        syserr("node reader preliminary protocol mutex_unlock failed\n");
    }

    if (pthread_mutex_lock(&node->thread_counter)) {
        syserr("node reader preliminary protocol mutex_lock failed\n");
    }

    node->subtree_threads++;

    if (pthread_mutex_unlock(&node->thread_counter)) {
        syserr("node reader preliminary protocol mutex_unlock failed\n");
    }

    return node->emergency_pass;
}

void node_reader_final_protocol(Node* node) {
    if (pthread_mutex_lock(&node->lock)) {
        syserr("node reader final protocol mutex_lock failed\n");
    }

    node->readers_inside--;
    node->change = WRITERS;

    if (!node->readers_inside) {
        if (pthread_cond_signal(&node->writers)) {
            syserr("node reader final protocol cond_signal failed\n");
        }
    }

    if (pthread_mutex_unlock(&node->lock)) {
        syserr("node reader final protocol mutex_unlock failed\n");
    }
}

bool node_writer_preliminary_protocol(Node* node) {
    if (pthread_mutex_lock(&node->lock)) {
        syserr("node writer preliminary protocol mutex_lock failed\n");
    }

    while (((node->writers_inside + node->readers_inside > 0) ||
           (node->readers_waiting > 0 && node->change == READERS)) &&
           node->emergency_pass == false)
    {
        node->writers_waiting++;

        if (pthread_cond_wait(&node->writers, &node->lock)) {
            syserr("node writer preliminary protocol cond_wait failed\n");
        }

        node->writers_waiting--;
    }

    node->writers_inside++;

    if (pthread_mutex_unlock(&node->lock)) {
        syserr("node writer preliminary protocol mutex_unlock failed\n");
    }

    if (pthread_mutex_lock(&node->thread_counter)) {
        syserr("node writer preliminary protocol mutex_lock failed\n");
    }

    node->subtree_threads++;

    while (node->subtree_threads > 1 && node->emergency_pass == false) {
        if (pthread_cond_wait(&node->private_access, &node->thread_counter)) {
            syserr("node writer preliminary protocol cond_wait failed\n");
        }
    }

    if (pthread_mutex_unlock(&node->thread_counter)) {
        syserr("node writer preliminary protocol mutex_unlock failed\n");
    }

    return node->emergency_pass;
}

void node_writer_final_protocol(Node* node) {
    if (pthread_mutex_lock(&node->lock)) {
        syserr("node writer final protocol mutex_lock failed\n");
    }

    node->writers_inside--;
    node->change = READERS;

    if (node->readers_waiting) {
        if (pthread_cond_signal(&node->readers)) {
            syserr("node writer final protocol cond_signal readers failed\n");
        }
    }
    else if (pthread_cond_signal(&node->writers)) {
        syserr("node writer final protocol cond_signal writers failed\n");
    }

    if (pthread_mutex_unlock(&node->lock)) {
        syserr("node writer final protocol mutex_unlock failed\n");
    }
}

void node_discard_waiting(Node* node) {
    if (pthread_mutex_lock(&node->lock)) {
        syserr("discard mutex_lock lock failed\n");
    }

    if (pthread_mutex_lock(&node->thread_counter)) {
        syserr("discard mutex_lock failed\n");
    }

    node->emergency_pass = true;

    if (pthread_cond_signal(&node->readers)) {
        syserr("discard cond_signal readers failed\n");
    }

    if (pthread_cond_signal(&node->writers)) {
        syserr("discard cond_signal writers failed\n");
    }

    if (pthread_mutex_unlock(&node->lock)) {
        syserr("discard mutex_unlock lock failed\n");
    }

    if (pthread_cond_signal(&node->private_access)) {
        syserr("discard cond_signal private failed\n");
    }

    while (node->safe_hmap_get_count > 1) {
        if (pthread_cond_wait(&node->private_access, &node->thread_counter)) {
            syserr("discard cond_wait failed\n");
        }
    }

    if (pthread_mutex_unlock(&node->thread_counter)) {
        syserr("discard mutex_unlock failed\n");
    }

    node->emergency_pass = false;
}