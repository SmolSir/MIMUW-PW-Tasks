#include "path_utils.h"
#include "err.h"

#define READERS false
#define WRITERS true

// A structure representing a thread-safe HashMap. All of its functions are
// simply calls for some original HashMap and path_utils functions wrapped
// in readers-writers problem solution. "Readers" are hmap_get, hmap_size and
// make_map_contents_string. "Writers" are hmap_insert and hmap_remove.
typedef struct SafeHashMap SafeHashMap; // Let "SafeHashMap" mean the same as "struct SafeHashMap".

struct SafeHashMap {
    HashMap* map;
    
    pthread_mutex_t lock;
    pthread_cond_t readers;
    pthread_cond_t writers;
    
    int readers_inside, writers_inside, readers_waiting, writers_waiting;
    bool change; // READERS is false, WRITERS is true;
};


// Constructor for the structure.
SafeHashMap* safe_hmap_new();

// Destructor for the structure.
void safe_hmap_free(SafeHashMap* safe_map);

// Readers-writers protocols.
void map_reader_preliminary_protocol(SafeHashMap* safe_map);

void map_reader_final_protocol(SafeHashMap* safe_map);

void map_writer_preliminary_protocol(SafeHashMap* safe_map);

void map_writer_final_protocol(SafeHashMap* safe_map);


// Thread-safe functions definitions. safe_hmap_get is inside Node module,
// as it operates on Node's in our task.
bool safe_hmap_insert(SafeHashMap* safe_map, const char* key, void* value);

bool safe_hmap_remove(SafeHashMap* safe_map, const char* key);

size_t safe_hmap_size(SafeHashMap* safe_map);

char* safe_make_map_contents_string(SafeHashMap* safe_map);