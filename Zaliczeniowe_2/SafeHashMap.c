#include <pthread.h>
#include <stdlib.h>

#include "SafeHashMap.h"

SafeHashMap* safe_hmap_new() {
    SafeHashMap* safe_map = safe_malloc(sizeof(SafeHashMap));

    if (pthread_mutex_init(&safe_map->lock, 0)) {
        syserr("safe hashmap lock init failed\n");
    }
    if (pthread_cond_init(&safe_map->readers, 0)) {
        syserr("safe hashmap readers init failed\n");
    }
    if (pthread_cond_init(&safe_map->writers, 0)) {
        syserr("safe hashmap writers init failed\n");
    }

    safe_map->map = hmap_new();

    safe_map->readers_inside = 0;
    safe_map->readers_waiting = 0;
    safe_map->writers_inside = 0;
    safe_map->writers_waiting = 0;
    safe_map->change = WRITERS;

    return safe_map;
}

void safe_hmap_free(SafeHashMap* safe_map) {
    if (pthread_mutex_destroy(&safe_map->lock)) {
        syserr("safe hashmap lock destroy failed\n");
    }
    if (pthread_cond_destroy(&safe_map->readers)) {
        syserr("safe hashmap readers destroy failed\n");
    }
    if (pthread_cond_destroy(&safe_map->writers)) {
        syserr("safe hashmap writers destroy failed\n");
    }

    hmap_free(safe_map->map);
    free(safe_map);
}

void map_reader_preliminary_protocol(SafeHashMap* safe_map) {
    if (pthread_mutex_lock(&safe_map->lock)) {
        syserr("safe hashmap reader preliminary protocol mutex_lock failed\n");
    }

    while (safe_map->writers_inside > 0 ||
          (safe_map->writers_waiting > 0 && safe_map->change == WRITERS))
    {
        safe_map->readers_waiting++;

        if (pthread_cond_wait(&safe_map->readers, &safe_map->lock)) {
            syserr("safe hashmap reader preliminary protocol cond_wait failed\n");
        }

        safe_map->readers_waiting--;
    }

    safe_map->readers_inside++;

    if (!safe_map->writers_waiting) {
        if (pthread_cond_signal(&safe_map->readers)) {
            syserr("safe hashmap reader preliminary protocol cond_signal failed\n");
        }
    }

    if (pthread_mutex_unlock(&safe_map->lock)) {
        syserr("safe hashmap reader preliminary protocol mutex_unlock failed\n");
    }
}

void map_reader_final_protocol(SafeHashMap* safe_map) {
    if (pthread_mutex_lock(&safe_map->lock)) {
        syserr("safe hashmap reader final protocol mutex_lock failed\n");
    }

    safe_map->readers_inside--;
    safe_map->change = WRITERS;

    if (!safe_map->readers_inside) {
        if (pthread_cond_signal(&safe_map->writers)) {
            syserr("safe hashmap reader final protocol cond_signal failed\n");
        }
    }

    if (pthread_mutex_unlock(&safe_map->lock)) {
        syserr("safe hashmap reader final protocol mutex_unlock failed\n");
    }
}

void map_writer_preliminary_protocol(SafeHashMap* safe_map) {
    if (pthread_mutex_lock(&safe_map->lock)) {
        syserr("safe hashmap writer preliminary protocol mutex_lock failed\n");
    }

    while (safe_map->writers_inside + safe_map->readers_inside > 0 ||
          (safe_map->readers_waiting > 0 && safe_map->change == READERS))
    {
        safe_map->writers_waiting++;

        if (pthread_cond_wait(&safe_map->writers, &safe_map->lock)) {
            syserr("safe hashmap writer preliminary protocol cond_wait failed\n");
        }

        safe_map->writers_waiting--;
    }

    safe_map->writers_inside++;

    if (pthread_mutex_unlock(&safe_map->lock)) {
        syserr("safe hashmap writer preliminary protocol mutex_unlock failed\n");
    }
}

void map_writer_final_protocol(SafeHashMap* safe_map) {
    if (pthread_mutex_lock(&safe_map->lock)) {
        syserr("safe hashmap writer final protocol mutex_lock failed\n");
    }

    safe_map->writers_inside--;
    safe_map->change = READERS;

    if (safe_map->readers_waiting) {
        if (pthread_cond_signal(&safe_map->readers)) {
            syserr("safe hashmap writer final protocol cond_signal readers failed\n");
        }
    }
    else if (pthread_cond_signal(&safe_map->writers)) {
        syserr("safe hashmap writer final protocol cond_signal writers failed\n");
    }

    if (pthread_mutex_unlock(&safe_map->lock)) {
        syserr("safe hashmap writer final protocol mutex_unlock failed\n");
    }
}

bool safe_hmap_insert(SafeHashMap* safe_map, const char* key, void* value) {
    map_writer_preliminary_protocol(safe_map);
    bool result = hmap_insert(safe_map->map, key, value);
    map_writer_final_protocol(safe_map);

    return result;
}

bool safe_hmap_remove(SafeHashMap* safe_map, const char* key) {
    map_writer_preliminary_protocol(safe_map);
    bool result = hmap_remove(safe_map->map, key);
    map_writer_final_protocol(safe_map);

    return result;
}

size_t safe_hmap_size(SafeHashMap* safe_map) {
    map_reader_preliminary_protocol(safe_map);
    size_t result = hmap_size(safe_map->map);
    map_reader_final_protocol(safe_map);

    return result;
}

char* safe_make_map_contents_string(SafeHashMap* safe_map) {
    map_reader_preliminary_protocol(safe_map);
    char* result = make_map_contents_string(safe_map->map);
    map_reader_final_protocol(safe_map);

    return result;
}