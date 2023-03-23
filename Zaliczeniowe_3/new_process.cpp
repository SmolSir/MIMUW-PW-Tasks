#include <unistd.h>
#include <fcntl.h>

#include "lib/infint/InfInt.h"
#include "collatz.hpp"
#include "sharedresults.hpp"

int main(int argc, char* argv[])
{

    int r_shared_desc;
    int task_read_desc;
    int result_write_desc;

    uint64_t task_count;
    std::vector<InfInt> tasks;
    std::vector<uint64_t> results;


    if ((task_read_desc = open(argv[1], O_RDONLY)) == -1)
    {
        printf("Error in open task_read_desc in PROC\n");
        exit(-1);
    }

    if (read(task_read_desc, &task_count, sizeof(uint64_t)) == -1)
    {
        printf("Error in read task_count in PROC\n");
        exit(-1);
    }

    tasks.resize(task_count);
    results.resize(task_count);

    for (uint64_t task_ID = 0; task_ID < task_count; task_ID++)
    {
        size_t task_digits;
        std::string task_string;

        if (read(task_read_desc, &task_digits, sizeof(size_t)) == -1)
        {
            printf("Error in read task_digits in PROC\n");
            exit(-1);
        }

        task_string.resize(task_digits);

        if (read(task_read_desc, (void*) task_string.c_str(), task_digits) == -1)
        {
            printf("Error in read task_string in PROC\n");
            exit(-1);
        }

        tasks[task_ID] = InfInt(task_string.c_str());
    }

    if (close(task_read_desc))
    {
        printf("Error in close task_read_desc in PROC\n");
        exit(-1);
    }


    for (uint64_t task_ID = 0; task_ID < task_count; task_ID++)
    {
        results[task_ID] = calcCollatz(tasks[task_ID]);
    }


    if ((result_write_desc = open(argv[2], O_WRONLY)) == -1)
    {
        printf("Error in open result_write_desc in PROC\n");
        exit(-1);
    }

    for (const auto& result : results)
    {
        if (write(result_write_desc, &result, sizeof(uint64_t)) == -1)
        {
            printf("Error in write result in PROC\n");
            exit(-1);
        }
    }

    if (close(result_write_desc))
    {
        printf("Error in close result_write_desc in PROC\n");
        exit(-1);
    }


    exit(0);
}