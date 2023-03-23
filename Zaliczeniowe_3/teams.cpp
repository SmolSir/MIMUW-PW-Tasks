#include <deque>
#include <future>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <fcntl.h>
#include <filesystem>

#include "teams.hpp"
#include "contest.hpp"
#include "collatz.hpp"


ContestResult TeamNewThreads::runContestImpl(ContestInput const & contestInput)
{
    ContestResult r;
    std::vector<std::promise<uint64_t>> r_promises;
    std::vector<std::future<uint64_t>> r_futures;

    r.resize(contestInput.size());
    r_promises.resize(contestInput.size());
    r_futures.resize(contestInput.size());

    std::deque<std::thread> threads;
    uint64_t task_ID = 0;
    uint64_t oldest_task_ID = 0;

    for (uint64_t i = 0; i < contestInput.size(); i++)
    {
        r_futures[i] = r_promises[i].get_future();
    }

    auto F = [this](const InfInt& n, std::promise<uint64_t>& promise)
    {
        auto r_shared = this->getSharedResults();
        uint64_t result = r_shared ? r_shared->sharedCollatz(n) : calcCollatz(n);
        promise.set_value(result);
    };

    for (auto task : contestInput)
    {
        if (threads.size() == this->getSize())
        {
            uint64_t value = r_futures[oldest_task_ID].get();
            r[oldest_task_ID] = value;
            threads.front().join();
            threads.pop_front();
            oldest_task_ID++;
        }

        threads.push_back(createThread(F, task, std::ref(r_promises[task_ID])));
        task_ID++;
    }


    while (!threads.empty())
    {
        r[oldest_task_ID] = r_futures[oldest_task_ID].get();
        threads.front().join();
        threads.pop_front();
        oldest_task_ID++;
    }

    return r;
}

ContestResult TeamConstThreads::runContestImpl(ContestInput const & contestInput)
{
    ContestResult r;
    r.resize(contestInput.size());

    std::vector<InfInt> tasks[this->getSize()];
    std::vector<std::promise<uint64_t>> r_promises[this->getSize()];
    std::vector<std::future<uint64_t>> r_futures(contestInput.size());

    std::deque<std::thread> threads;
    
    
    for (uint64_t i = 0; i < contestInput.size(); i++)
    {
        tasks[i % this->getSize()].push_back(contestInput[i]);
        
        r_promises[i % this->getSize()].emplace_back();
        r_futures[i] = r_promises[i % this->getSize()].back().get_future();
    }

    auto F = 
            [this]
            (const std::vector<InfInt>& tasks,
                   std::vector<std::promise<uint64_t>>& promises)
    {
        auto r_shared = this->getSharedResults();
        for (uint64_t i = 0; i < tasks.size(); i++)
        {
            uint64_t result = r_shared ? r_shared->sharedCollatz(tasks[i]) : calcCollatz(tasks[i]);
            promises[i].set_value(result);
        }
    };
    
    for (uint32_t i = 0; i < this->getSize(); i++)
    {
        threads.push_back(createThread(F, tasks[i], std::ref(r_promises[i])));
    }

    uint64_t task_ID = 0;

    for (auto& future : r_futures)
    {
        r[task_ID] = future.get();
        task_ID++;
    }

    while (!threads.empty())
    {
        threads.front().join();
        threads.pop_front();
    }

    return r;
}

ContestResult TeamPool::runContest(ContestInput const & contestInput)
{
    ContestResult r;

    std::vector<std::future<uint64_t>> r_futures;

    auto F = [this](const InfInt& n)
    {
        auto r_shared = this->getSharedResults();
        return r_shared ? r_shared->sharedCollatz(n) : calcCollatz(n);
    };

    for (const auto& task : contestInput)
    {
        r_futures.push_back(pool.push(F, task));
    }

    r = cxxpool::get(r_futures.begin(), r_futures.end());

    return r;
}


ContestResult TeamNewProcesses::runContest(ContestInput const & contestInput)
{
    ContestResult r;
    r.resize(contestInput.size());

    int r_shared_desc;
    int task_write_desc;
    int result_read_desc;

    std::string r_shared_file_name = "/tmp/r_shared";
    std::string task_file_prefix = "/tmp/task-";
    std::string result_file_prefix = "/tmp/result-";

    std::string task_file_names[this->getSize()];
    std::string result_file_names[this->getSize()];

    uint64_t oldest_task_ID = 0;
    uint64_t task_count = 1;


    for (uint32_t ID = 0; ID < this->getSize(); ID++)
    {
        task_file_names[ID] = task_file_prefix + std::to_string(ID);
        result_file_names[ID] = result_file_prefix + std::to_string(ID);


        try
        {
            std::filesystem::remove(task_file_names[ID]);
            std::filesystem::remove(result_file_names[ID]);
        }
        catch(const std::filesystem::filesystem_error& err)
        {
            printf("filesystem error: %s\n", err.what());
            exit(-1);
        }

        if (mkfifo(task_file_names[ID].c_str(), 0755) == -1)
        {
            printf("Error in mkfifo %s\n", task_file_names[ID].c_str());
            exit(-1);
        }

        if (mkfifo(result_file_names[ID].c_str(), 0755) == -1)
        {
            printf("Error in mkfifo %s\n", result_file_names[ID].c_str());
            exit(-1);
        }
    }


    for (uint64_t task_ID = 0; task_ID < contestInput.size(); task_ID++)
    {
        if (task_ID - oldest_task_ID == this->getSize())
        {
            if ((result_read_desc = open(result_file_names[oldest_task_ID % this->getSize()].c_str(), O_RDONLY)) == -1)
            {
                printf("Error in open result_read_desc\n");
                exit(-1);
            }

            if (read(result_read_desc, &r[oldest_task_ID], sizeof(uint64_t)) == -1)
            {
                printf("Error in read result[%ld]\n", oldest_task_ID);
                exit(-1);
            }

            if (close(result_read_desc))
            {
                printf("Error in close result_read_desc\n");
                exit(-1);
            }

            if (wait(0) == -1)
            {
                printf("Error in wait\n");
                exit(-1);
            }

            oldest_task_ID++;
        }


        switch (fork())
        {
            case -1:
                printf("Error in fork\n");
                exit(-1);

            case 0:
                execl(
                        "./new_process",
                        r_shared_file_name.c_str(),
                        task_file_names[task_ID % this->getSize()].c_str(),
                        result_file_names[task_ID % this->getSize()].c_str(),
                        NULL);

                printf("Error in execl\n");
                exit(-1);

            default:
                if ((task_write_desc = open(task_file_names[task_ID % this->getSize()].c_str(), O_WRONLY)) == -1)
                {
                    printf("Error in open task_write_desc\n");
                    exit(-1);
                }

                if (write(task_write_desc, &task_count, sizeof(uint64_t)) == -1)
                {
                    printf("Error in write task_count\n");
                    exit(-1);
                }

                size_t task_digits = contestInput[task_ID].numberOfDigits();

                if (write(task_write_desc, &task_digits, sizeof(size_t)) == -1)
                {
                    printf("Error in write task_digits\n");
                    exit(-1);
                }

                if (write(task_write_desc, contestInput[task_ID].toString().c_str(), task_digits) == -1)
                {
                    printf("Error in write task_string\n");
                    exit(-1);
                }

                if (close(task_write_desc))
                {
                    printf("Error in close task_write_desc\n");
                    exit(-1);
                }
        }
    }


    while (oldest_task_ID != contestInput.size())
    {
        if ((result_read_desc = open(result_file_names[oldest_task_ID % this->getSize()].c_str(), O_RDONLY)) == -1)
        {
            printf("Error in open result_read_desc\n");
            exit(-1);
        }

        if (read(result_read_desc, &r[oldest_task_ID], sizeof(uint64_t)) == -1)
        {
            printf("Error in read result[%ld]\n", oldest_task_ID);
            exit(-1);
        }

        if (close(result_read_desc))
        {
            printf("Error in close result_read_desc\n");
            exit(-1);
        }

        if (wait(0) == -1)
        {
            printf("Error in wait\n");
            exit(-1);
        }

        oldest_task_ID++;
    }


    for (uint32_t ID = 0; ID < this->getSize(); ID++)
    {
        if (unlink(task_file_names[ID].c_str()))
        {
            printf("Error in unlink %s\n", task_file_names[ID].c_str());
            exit(-1);
        }

        if (unlink(result_file_names[ID].c_str()))
        {
            printf("Error in unlink %s\n", result_file_names[ID].c_str());
            exit(-1);
        }
    }

    return r;
}


ContestResult TeamConstProcesses::runContest(ContestInput const & contestInput)
{
    ContestResult r;
    r.resize(contestInput.size());

    int r_shared_desc;
    int task_write_desc;
    int result_read_desc;

    std::string r_shared_file_name = "/tmp/r_shared";
    std::string result_file_names[this->getSize()];

    uint64_t task_count;
    std::vector<InfInt> tasks[this->getSize()];


    for (uint64_t i = 0; i < contestInput.size(); i++)
    {
        tasks[i % this->getSize()].push_back(contestInput[i]);
    }


    for (uint32_t ID = 0; ID < this->getSize(); ID++)
    {
        std::string task_file_name = "/tmp/task-" + std::to_string(ID);
        std::string result_file_name = "/tmp/result-" + std::to_string(ID);

        result_file_names[ID] = result_file_name;

        try
        {
            std::filesystem::remove(task_file_name);
            std::filesystem::remove(result_file_name);
        }
        catch(const std::filesystem::filesystem_error& err)
        {
            printf("filesystem error: %s\n", err.what());
            exit(-1);
        }

        if (mkfifo(task_file_name.c_str(), 0755) == -1)
        {
            printf("Error in mkfifo %s\n", task_file_name.c_str());
            exit(-1);
        }

        if (mkfifo(result_file_name.c_str(), 0755) == -1)
        {
            printf("Error in mkfifo %s\n", result_file_name.c_str());
            exit(-1);
        }

        switch (fork())
        {
            case -1:
                printf("Error in fork\n");
                exit(-1);

            case 0:
                execl(
                        "./new_process",
                        r_shared_file_name.c_str(),
                        task_file_name.c_str(),
                        result_file_name.c_str(),
                        NULL);

                printf("Error in execl\n");
                exit(-1);

            default:
                if ((task_write_desc = open(task_file_name.c_str(), O_WRONLY)) == -1)
                {
                    printf("Error in open task_write_desc\n");
                    exit(-1);
                }

                task_count = tasks[ID].size();

                if (write(task_write_desc, &task_count, sizeof(uint64_t)) == -1)
                {
                    printf("Error in write task_count\n");
                    exit(-1);
                }

                for (const auto& task : tasks[ID]) {

                    size_t task_digits = task.numberOfDigits();

                    if (write(task_write_desc, &task_digits, sizeof(size_t)) == -1)
                    {
                        printf("Error in write task_digits\n");
                        exit(-1);
                    }

                    if (write(task_write_desc, task.toString().c_str(), task_digits) == -1)
                    {
                        printf("Error in write task_string\n");
                        exit(-1);
                    }
                }

                if (close(task_write_desc))
                {
                    printf("Error in close task_write_desc\n");
                    exit(-1);
                }

                if (unlink(task_file_name.c_str()))
                {
                    printf("Error in unlink %s\n", task_file_name.c_str());
                    exit(-1);
                }
        }
    }


    for (uint32_t ID = 0; ID < this->getSize(); ID++)
    {
        if ((result_read_desc = open(result_file_names[ID].c_str(), O_RDONLY)) == -1)
        {
            printf("Error in open result_read_desc\n");
            exit(-1);
        }

        for (uint64_t task_ID = ID; task_ID < contestInput.size(); task_ID += this->getSize())
        {
            if (read(result_read_desc, &r[task_ID], sizeof(uint64_t)) == -1)
            {
                printf("Error in read result[%ld]\n", task_ID);
                exit(-1);
            }
        }

        if (close(result_read_desc))
        {
            printf("Error in close result_read_desc\n");
            exit(-1);
        }

        if (unlink(result_file_names[ID].c_str()))
        {
            printf("Error in unlink %s\n", result_file_names[ID].c_str());
            exit(-1);
        }

        if (wait(0) == -1)
        {
            printf("Error in wait\n");
            exit(-1);
        }
    }

    return r;
}

ContestResult TeamAsync::runContest(ContestInput const & contestInput)
{
    ContestResult r;

    std::vector<std::future<uint64_t>> r_futures;

    auto F = [this](const InfInt& n)
    {
        auto r_shared = this->getSharedResults();
        return r_shared ? r_shared->sharedCollatz(n) : calcCollatz(n);
    };

    for (auto task : contestInput)
    {
        r_futures.push_back(std::async(F, task));
    }

    r = cxxpool::get(r_futures.begin(), r_futures.end());

    return r;
}
