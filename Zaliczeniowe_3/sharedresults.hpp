#ifndef SHAREDRESULTS_HPP
#define SHAREDRESULTS_HPP

#include <shared_mutex>
#include <unordered_map>

struct InfInt_hash
{
    std::size_t operator () (InfInt const &v) const
    {
        return std::hash<std::string>()(v.toString());
    }
};

class SharedResults
{
public:

    SharedResults() : _shared_mutex(), _results() {}


    uint64_t get(const InfInt& key)
    {
        _shared_mutex.lock_shared();
        auto result = _results.contains(key) ? _results[key] : 0;
        _shared_mutex.unlock_shared();

        return result;
    }

    void insert(const InfInt& key, uint64_t value)
    {
        _shared_mutex.lock();
        _results.insert({key, value});
        _shared_mutex.unlock();
    }

    inline uint64_t sharedCollatz(InfInt n)
    {
        // It's ok even if the value overflow
        assert(n > 0);

        uint64_t score = get(n);
        if (n == 1 || score)
        {
            return score;
        }

        InfInt successor = (n % 2 == 1) ? (InfInt)3 * n + 1 : n / 2;

        uint64_t successor_score = sharedCollatz(successor);
        insert(n, successor_score + 1);

        return successor_score + 1;
    }

private:
    std::unordered_map<InfInt, uint64_t, InfInt_hash> _results;
    std::shared_mutex _shared_mutex;
};

#endif