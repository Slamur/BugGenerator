#pragma GCC optimize("O2,unroll-loops")

#include <bits/stdc++.h>

const char WALL = '#';
const char EMPTY = '.';

const int STEPS_COUNT = 4;
int dx[STEPS_COUNT] = { 1, 0, -1, 0 };
int dy[STEPS_COUNT] = { 0, 1, 0, -1 };

std::default_random_engine random_engine(std::time(NULL));
std::uniform_real_distribution<double> probability_distribution;
std::uniform_int_distribution<int> cell_distribution;

int rand() {
    return cell_distribution(random_engine);
}

template<int x_size, int y_size>
class field_t {
private:
    using cnt_t = uint64_t;
    cnt_t _counts[x_size][y_size];

    short _q[x_size * y_size];
    uint _used[x_size][y_size];
    uint _check_path_iteration;

    uint _costs[x_size][y_size];
    uint _connect_costs[x_size][y_size];
    int _parents[x_size][y_size];
public:
    bool wall[x_size][y_size];
    cnt_t result;

    field_t()
    { 
        _check_path_iteration = 0;

        for (int x = 0; x < x_size; ++x) {
            for (int y = 0; y < y_size; ++y) {
                wall[x][y] = false;
                _used[x][y] = _check_path_iteration;
            }
        }

        for (int x = 0; x < x_size; ++x) {
            wall[x][0] = wall[x][y_size - 1] = true;
        }

        for (int y = 0; y < y_size; ++y) {
            wall[0][y] = wall[x_size - 1][y] = true;
        }

        result = calculate();
    }

    field_t<x_size, y_size>& operator=(const field_t<x_size, y_size> &other) {
        for (int x = 0; x < x_size; ++x) {
            for (int y = 0; y < y_size; ++y) {
                wall[x][y] = other.wall[x][y];
            }
        }

        result = other.result;

        return *this;
    }

    bool check_path() {
        if (wall[1][1]) {
            return false;
        }

        ++_check_path_iteration;

        _used[1][1] = _check_path_iteration;
        _q[0] = y_size + 1;

        for (int it = 0, q_size = 1; it < q_size; ++it) {
            int v = _q[it];
            int x = v / y_size, y = v - y_size * x;

            for (int step = 0; step < STEPS_COUNT; ++step) {
                int to_x = x + dx[step], to_y = y + dy[step];

                if (wall[to_x][to_y]) continue;
                if (_used[to_x][to_y] == _check_path_iteration) continue;

                _q[q_size++] = to_x * y_size + to_y;
                _used[to_x][to_y] = _check_path_iteration;
            }
        }

        return _used[x_size - 2][y_size - 2] == _check_path_iteration;
    }

    cnt_t calculate() {
        if (!check_path()) {
            return result = 0;
        }

        for (int x = 0; x < x_size; ++x) {
            for (int y = 0; y < y_size; ++y) {
                _counts[x][y] = 0;
            }
        }

        int dir = 0;
        int x = 1, y = 1;

        result = 0;
        while (x + 2 != x_size || y + 2 != y_size) {
            _counts[x][y]++;

            cnt_t min_cnt = 1e18;
            for (int step = 0; step < STEPS_COUNT; ++step) {
                int to_x = x + dx[step], to_y = y + dy[step];
                if (wall[to_x][to_y]) continue;

                min_cnt = std::min(min_cnt, _counts[to_x][to_y]);
            }

            int selected_dir = -1;
            int next_x = x + dx[dir], next_y = y + dy[dir];

            if (!wall[next_x][next_y] && _counts[next_x][next_y] == min_cnt) {
                selected_dir = dir;
            } else {
                for (int step = 0; step < STEPS_COUNT; ++step) {
                    int to_x = x + dx[step], to_y = y + dy[step];
                    if (wall[to_x][to_y]) continue;

                    if (_counts[to_x][to_y] == min_cnt) {
                        selected_dir = step;
                        break;
                    }
                }
            }

            dir = selected_dir;
            x += dx[dir];
            y += dy[dir];
            ++result;
        }

        return result;
    }

    void connect() {
        if (check_path()) return;

        for (int x = 0; x < x_size; ++x) {
            for (int y = 0; y < y_size; ++y) {
                int edge_distance = std::min(
                    std::min(x, x_size - x - 1),
                    std::min(y, y_size - y - 1)
                );
                bool is_edge = edge_distance == 0;

                _costs[x][y] = is_edge ? 0 : wall[x][y] * (rand() % 1'000'000);
                _connect_costs[x][y] = is_edge ? 0 : -1;
                _parents[x][y] = -1;
            }
        }

        _connect_costs[1][1] = _costs[1][1];

        std::priority_queue<std::pair<int, short>> _pq;
        _pq.emplace(-_connect_costs[1][1], y_size + 1);

        while (_pq.size() > 0) {
            auto [distance, v] = _pq.top();
            _pq.pop();

            distance = -distance;

            int x = v / y_size, y = v - y_size * x;
            if (distance > _connect_costs[x][y]) continue;

            if (x == x_size - 2 && y == y_size - 2) break;

            for (int step = 0; step < STEPS_COUNT; ++step) {
                int to_x = x + dx[step], to_y = y + dy[step];

                int to_cost = _costs[to_x][to_y];
                if (_connect_costs[to_x][to_y] <= distance + to_cost) continue;

                _connect_costs[to_x][to_y] = distance + to_cost;
                _parents[to_x][to_y] = v;
                _pq.emplace(-_connect_costs[to_x][to_y], to_x * y_size + to_y);
            }
        }

        for (int x = x_size - 2, y = y_size - 2; ; ) {
            wall[x][y] = false;

            int parent = _parents[x][y];
            if (-1 == parent) break;

            x = parent / y_size;
            y = parent - y_size * x;
        }
    }

    void randomize(int wall_prob) {
        for (int x = 1; x < x_size - 1; ++x) {
            for (int y = 1; y < y_size - 1; ++y) {
                wall[x][y] = rand() % 100 <= wall_prob;
            }
        }

        connect();
        calculate();
    }

    short _gen_cell() {
        short total = (x_size - 2) * (y_size - 2) - 2;
        int v = rand() % total;

        v++;

        return v;
    }

    void mutate() {
        int v = _gen_cell();
        int x = v / (y_size - 2) + 1, y = v % (y_size - 2) + 1;

        wall[x][y] ^= true;
        
        connect();
        calculate();
    }

    void simulated_annealing() {
        int iterations = (x_size * y_size) * 1e2;
        double lambda = 1 - 1e-3;

        double temperature = 1;

        cnt_t cur_result = result;
        for (int it = 0; it < iterations; ++it) {
            temperature *= lambda;

            int v = _gen_cell();
            int x = v / (y_size - 2) + 1, y = v % (y_size - 2) + 1;

            wall[x][y] ^= true;

            cnt_t new_result = calculate();
            
            bool change_state = new_result > cur_result;
            if (!change_state) {
                double probability = probability_distribution(random_engine);
                double delta = double(new_result) - double(cur_result);

                change_state = new_result > 0 && probability < exp(delta / temperature);
            }

            if (change_state) {
                cur_result = new_result;
            } else {
                wall[x][y] ^= true;
            }
        }

        connect();
        calculate();
    }
};

const int X_SIZE = 21;
const int Y_SIZE = 31;

using Field = field_t<X_SIZE, Y_SIZE>;

std::ostream& operator<<(std::ostream &out, const Field &field) {
    out << field.result << "\n";

    for (int x = 0; x < X_SIZE; ++x) {
        for (int y = 0; y < Y_SIZE; ++y) {
            out << (field.wall[x][y] ? WALL : EMPTY);
        }
        out << "\n";
    }

    return out;
}

std::istream& operator>>(std::istream &in, Field &field) {
    int score;
    if (!(in >> score)) {
        return in;
    }

    for (int x = 0; x < X_SIZE; ++x) {
        std::string row;
        in >> row;
        for (int y = 0; y < Y_SIZE; ++y) {
            field.wall[x][y] = (row[y] == WALL);
        }
    }

    field.calculate();
    
    return in;
}

Field read_best() {
    Field field;

    std::ifstream f_in("best.txt", std::ios_base::openmode::_S_in);
    f_in >> field;

    return field;
}

void print_best(const Field& field) {
    std::ofstream f_out("best.txt", std::ios_base::openmode::_S_out);
    f_out << field;
    f_out.flush();
}

std::vector<Field> read_starts() {
    std::ifstream f_in("starts.txt", std::ios_base::openmode::_S_in);
    
    std::vector<Field> starts;

    for (Field field; !f_in.eof(); ) {
        std::cout << "Reading field" << "\n";
        f_in >> field;
        std::cout << "Field result is " << field.result << "\n";

        starts.push_back(field);
    }

    // eof moment
    if (starts.size() > 2 && starts.back().result == starts[starts.size() - 2].result) {
        starts.pop_back();
    }

    return starts;
}

void print_start(const Field& field) {
    std::ofstream f_out("starts.txt", std::ios_base::openmode::_S_app);
    
    f_out << field;
    f_out.flush();
}

bool operator<(const Field& left, const Field& right) {
    return left.result < right.result;
}

const int BLOCK_SIZE = 5;

Field operator%(const Field& left, const Field& right) {
    Field child;

    for (int x_start = 1; x_start + 1 < X_SIZE; x_start += BLOCK_SIZE) {
        for (int y_start = 1; y_start + 1 < Y_SIZE; y_start += BLOCK_SIZE) {
            bool is_left = rand() % 2; //100 < left_prob;
            auto& source = is_left ? left : right;
            for (int dx = 0, x = x_start; dx < BLOCK_SIZE && x + 1 < X_SIZE; ++dx, ++x) {
                for (int dy = 0, y = y_start; dy < BLOCK_SIZE && y + 1 < Y_SIZE; ++dy, ++y) {
                    child.wall[x][y] = source.wall[x][y];
                }
            }
        }
    }

    child.connect();
    child.calculate();

    return child;
}

std::set<Field> initialize_fields(const std::vector<Field>& starts) {
    int random_size = 10, random_sa_size = random_size / 2;
    int empty_sa_size = 5, empty_size = empty_sa_size + 2;

    std::vector<Field> fields;

    int prefix = 0;

    {
        for (int i = 0; i < starts.size(); ++i) {
            fields.push_back(starts[i]);
        }

        prefix += starts.size();
    }

    {
        for (int i = 0; i < starts.size(); ++i) {
            fields.push_back(starts[i]);
            auto& start = fields.back();
            std::cout << "Started SA for " << i << " start (" << start.result << ")" << "\n";
            start.simulated_annealing();
            std::cout << "Finished SA for " << i << " start (" << start.result << ")" << "\n";
        }

        prefix += starts.size();
    }

    {
        for (int i = 0; i < random_size; ++i) {
            Field field;
            field.randomize((i % 5 + 1) * 10);
            fields.push_back(field);
        }

        for (int i = 0; i < random_sa_size; ++i) {
            fields[prefix + i].simulated_annealing();
        }

        prefix += random_size;
    }

    {
        for (int i = 0; i < empty_size; ++i) {
            fields.push_back(Field());
        }

        for (int i = 0; i < empty_sa_size; ++i) {
            fields[prefix + i].simulated_annealing();
        }

        prefix += empty_size;
    }

    return std::set<Field>(fields.begin(), fields.end());
}

Field generate_winner(const std::vector<Field>& starts) {
    int top_size = 20;
    size_t alive_children_size = 5;

    Field best_field;
    if (starts.size() > 0) {
        best_field = *std::max_element(starts.begin(), starts.end());
    }

    auto fields = initialize_fields(starts);

    int population_size = fields.size() + top_size;

    int not_changed_limit = 10;
    for (int iteration = 0, not_changed = 0; not_changed < not_changed_limit; ++not_changed, ++iteration) {
        std::cout << "Started iteration " << iteration << "\n";

        std::set<Field> new_fields;
        for (auto& left_field : fields) {
            std::set<Field> children;
            for (auto& right_field : fields) {
                auto child_field = left_field % right_field;
                child_field.mutate();
                children.insert(child_field);
            }

            int top_children_size = std::min(alive_children_size, children.size());
            new_fields.insert(std::prev(children.end(), top_children_size), children.end());
        }

        fields.insert(new_fields.begin(), new_fields.end());

        while (fields.size() > population_size) fields.erase(fields.begin());

        auto top_field = *fields.rbegin();

        if (starts.size() > 0) {
            std::cout << "Best after " << iteration << " iteration is " << top_field.result << "\n";
        }

        if (best_field < top_field) {
            best_field = top_field;

            std::cout << best_field << "\n";

            not_changed = 0;
        }
    }

    return best_field;
}

Field generate_winners(Field& best_field) {
    std::vector<Field> starts = read_starts();

    int winner_size = 20;

    int min_result_limit = 1e5;

    for (int iteration = 0; starts.size() < winner_size; ++iteration) {
        std::cout << "Started " << starts.size() + 1 << " generation (Iteration " << iteration << ")" << "\n";
        Field local_best_field = generate_winner(std::vector<Field>{});

        if (local_best_field.result < min_result_limit) {
            continue;
        }

        std::cout << "Local best: ";
        std::cout << local_best_field << "\n";
        print_start(local_best_field);

        starts.push_back(local_best_field);

        if (best_field < local_best_field) {
            std::cout << "Updated best: " << best_field.result << " < " << local_best_field.result << "\n";
            best_field = local_best_field;
            print_best(best_field);
        }
    }

    std::cout << "Generating champion" << "\n";
    auto champion = generate_winner(starts);

    std::cout << "Champion is: ";
    std::cout << champion << "\n";
    print_start(champion);

    if (best_field < champion) {
        std::cout << "Updated best by champion: " << best_field.result << " < " << champion.result << "\n";
        best_field = champion;
        print_best(best_field);
    }

    return best_field;
}

int main() {
    Field best_field = read_best();
    std::cout << best_field << "\n";

    generate_winners(best_field);

    return 0;
}
