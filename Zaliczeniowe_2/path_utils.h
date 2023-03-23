#include "HashMap.h"

// Max length of path (excluding terminating null character).
#define MAX_PATH_LENGTH 4095

// Max length of folder name (excluding terminating null character).
#define MAX_FOLDER_NAME_LENGTH 255

// Returns the pointer to newly allocated memory. In case of failure, ends the program with
// appropriate error message.
void* safe_malloc(size_t size);

// Returns:
// -1 if p1 leads to a more leftist Node than p2 in the tree,
//  0 if p1 leads to the same Node as p2 in the tree,
//  1 if p1 leads to a less leftist Node than p2 in the tree.
int compare_paths(const char* p1, const char* p2);

// Return whether a path is valid.
// Valid paths are '/'-separated sequences of folder names, always starting and ending with '/'.
// Valid paths have length at most MAX_PATH_LENGTH (and at least 1). Valid folder names are
// sequences of 'a'-'z' ASCII characters, of length from 1 to MAX_FOLDER_NAME_LENGTH.
bool is_path_valid(const char* path);

// Return the subpath obtained by removing the first component.
// Args:
// - `path`: should be a valid path (see `is_path_valid`).
// - `component`: if not NULL, should be a buffer of size at least MAX_FOLDER_NAME_LENGTH + 1.
//    Then the first component will be copied there (without any '/' characters).
// If path is "/", returns NULL and leaves `component` unchanged.
// Otherwise the returns a pointer into `path`, representing a valid subpath.
//
// This can be used to iterate over all components of a path:
//     char component[MAX_FOLDER_NAME_LENGTH + 1];
//     const char* subpath = path;
//     while (subpath = split_path(subpath, component))
//         printf("%s", component);
const char* split_path(const char* path, char* component);

// Return a copy of the subpath obtained by removing the last component.
// The caller should free the result, unless it is NULL.
// Args:
// - `path`: should be a valid path (see `is_path_valid`).
// - `component`: if not NULL, should be a buffer of size at least MAX_FOLDER_NAME_LENGTH + 1.
//    Then the last component will be copied there (without any '/' characters).
// If path is "/", returns NULL and leaves `component` unchanged.
// Otherwise the result is a valid path.
char* make_path_to_parent(const char* path, char* component);

// Returns a copy of the subpath consisting of initial folders that both paths share,
// ending with the last folder before first differing names.
// If paths have no shared folders, returns NULL.
// The caller should free the result, unless it is NULL.
// Args:
// - `p1`: should be a valid path (see `is_path_valid`).
// - `p2`: should be a valid path (see `is_path_valid`).
char* make_shared_path(const char* p1, const char* p2);

// Return an array containing all keys, lexicographically sorted.
// The result is null-terminated.
// Keys are not copied, they are only valid as long as the map.
// The caller should free the result.
const char** make_map_contents_array(HashMap* map);

// Return a string containing all keys in map, sorted, comma-separated.
// The result has no trailing comma. An empty map yields an empty string.
// The caller should free the result.
char* make_map_contents_string(HashMap* map);