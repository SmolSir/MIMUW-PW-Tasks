#include "SafeHashMap.h"

typedef struct Node Node; // Let "Node" mean the same as "struct Node".

// Structure representing one of the tree's nodes. Each node has:
// parent   - Node's direct parent. For root it is set to NULL.
// name     - Node's name used for finding it among parent's children.
// children - Map of all Node's direct children Nodes.

// lock           - Mutex for readers-writers protocols.
// thread_counter - Mutex for changing count of subtree's active threads.

// readers        - Conditional variable for readers-writers protocols.
// writers        - Same as readers.
// private_access - Conditional variable for a thread to wait until subtree has no
//                  active nodes. Related only to writers.

// all int variables - Counters for the things their names suggest.
// change            - Tells who should be using the library now.
// emergency_pass    - Tells other waiting threads that they should ignore others
//                     and immediately proceed with their preliminary protocols.
struct Node {
    Node* parent;
    char* name;
    SafeHashMap* children;

    pthread_mutex_t lock;
    pthread_mutex_t thread_counter;
    pthread_mutex_t move_map_writers_access;

    pthread_cond_t readers;
    pthread_cond_t writers;
    pthread_cond_t private_access;

    int readers_inside, writers_inside, readers_waiting, writers_waiting;
    int subtree_threads, safe_hmap_get_count;
    bool change; // READERS is false, WRITERS is true;
    bool emergency_pass;
};

// Creates a new Node and returns a pointer to it.
Node* node_new(Node* parent, char* name);

// This function frees all memory related to the node and it's children recursively.
// No need for thread-safeness since we are guaranteed that this function is
// called once by tree_free after regular tree function calls have finished.
void node_free(Node* node);

// Helper function for tree operations. Before return, tree functions recursively
// notify other threads waiting on their parent nodes that there is
// one less active thread in their subtrees. This information is necessary for
// node_writer protocols to function correctly.
// first - the Node from which recursion will start.
// last  - after reaching this Node, stop the recursion (this Node will NOT be notified).
// If last is NULL, then recursion will notify all nodes on path to the root, including it.
void notify_parent_nodes(Node* first, Node* last);

// hmap_get wrapped in SafeHashMap protocols. Exported here because uses Node information
// and in our task returns a pointer to Node anyway.
Node* safe_hmap_get(SafeHashMap* safe_map, const char* key);

// Readers-writers protocols with writers waiting for their subtree(s) operations
// to finish.
// Readers are tree_list and tree_create, since they do not remove any information
// about the tree's nodes.
// Writers are tree_remove and tree_move, since they can (re)move nodes and thus can
// cut threads active in their children from the tree, causing starvation. Because
// of this we provide their protocols with additional waiting for subtree threads
// to return before (re)moving any nodes.
bool node_reader_preliminary_protocol(Node* node);

void node_reader_final_protocol(Node* node);

bool node_writer_preliminary_protocol(Node* node);

void node_writer_final_protocol(Node* node);

// Helper function for writers. Calling it sets emergency_pass variable to true,
// causing all threads waiting on current Node to return appropriate error code inside
// their functions. After all waiting threads leave, caller thread continues work.
// node - Node from which all waiting threads shall be discarded.
void node_discard_waiting(Node* node);