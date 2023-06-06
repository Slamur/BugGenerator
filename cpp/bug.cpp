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
public:
    bool wall[x_size][y_size];
    cnt_t result;

    field_t()
    { 
        _check_path_iteration = 0;

        for (int x = 0; x < x_size; ++x) {
            for (int y = 0; y < y_size; ++y) {
                wall[x][y] = false;
                _used[x_size][y_size] = _check_path_iteration;
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

    void randomize(int wall_prob) {
        do {
            for (int x = 1; x < x_size - 1; ++x) {
                for (int y = 1; y < y_size - 1; ++y) {
                    wall[x][y] = rand() % 100 <= wall_prob;
                }
            }

            wall[1][1] = false;
            wall[x_size - 2][y_size - 2] = false;
        } while (!check_path());

        calculate();
    }

    short _gen_cell() {
        short total = (x_size - 2) * (y_size - 2) - 2;
        int v = rand() % total;

        v++;

        return v;
    }

    void mutate() {
        do {
            int v = _gen_cell();
            int x = v / (y_size - 2) + 1, y = v % (y_size - 2) + 1;

            wall[x][y] ^= true;
        } while (!check_path());

        calculate();
    }

    void simulated_annealing() {
        int iterations = (x_size * y_size) * 1e1;
        double lambda = 1 - 1e-2;

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

        calculate();
    }
};

const int X_SIZE = 21;
const int Y_SIZE = 31;

using Field = field_t<X_SIZE, Y_SIZE>;

std::ostream& operator<<(std::ostream &out, const Field &field) {
    for (int x = 0; x < X_SIZE; ++x) {
        for (int y = 0; y < Y_SIZE; ++y) {
            out << (field.wall[x][y] ? WALL : EMPTY);
        }
        out << "\n";
    }

    return out;
}

std::istream& operator>>(std::istream &in, Field &field) {
    for (int x = 0; x < X_SIZE; ++x) {
        std::string row;
        in >> row;
        for (int y = 0; y < Y_SIZE; ++y) {
            field.wall[x][y] = (row[y] == WALL);
        }
    }
    
    return in;
}

Field read_best() {
    Field field;

    std::ifstream f_in("best.txt", std::ios_base::openmode::_S_in);
    f_in >> field;

    field.calculate();

    return field;
}

void print_best(const Field& field) {
    std::ofstream f_out("best.txt", std::ios_base::openmode::_S_out);
    f_out << field;
    f_out.flush();
}

bool operator<(const Field& left, const Field& right) {
    return left.result < right.result;
}

const int BLOCK_SIZE = 5;

Field operator%(const Field& left, const Field& right) {
    //double left_prob = left.result / double(left.result + right.result) * 100;
    
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

    child.calculate();

    return child;
}

int main() {
    Field best_field = read_best();
    std::cout << best_field.result << "\n";
    std::cout << best_field << "\n";

    int population_size = 1e2;
    std::vector<Field> fields(population_size);
    fields[0] = best_field;

    while (true) {
        for (int i = population_size - 1; i > population_size - 10; --i) {
            fields[i] = best_field;
        }

        for (int i = population_size - 10, wall_prob = 0; i > population_size - 30; --i, ++wall_prob) {
            fields[i].randomize((wall_prob % 10 + 1) * 5);
        }

        for (int i = population_size - 30; i > population_size - 50; --i) {
            fields[i] = Field();
        }

        for (int i = population_size - 20; i > population_size - 40; --i) {
            fields[i].simulated_annealing();
        }

        int iterations = 1e2;
        for (int it = 0; it < iterations; ++it) {
            for (int left = 0; left < population_size; ++left) {
                for (int right = left + 1; right < population_size; ++right) {
                    auto child_field = fields[left] % fields[right];
                    child_field.mutate();
                    fields.push_back(child_field);
                }
            }

            std::sort(fields.begin(), fields.end());
            std::reverse(fields.begin(), fields.end());

            while (fields.size() > population_size) fields.pop_back();

            std::cout << it << " " << fields.front().result << "\n";
            if (best_field < fields.front()) {
                best_field = fields.front();

                std::cout << it << " " << best_field.result << "\n";
                std::cout << best_field << "\n";
            }
        }
        
        std::cout << best_field.result << "\n";
        std::cout << best_field << "\n";
        print_best(best_field);
    }

    return 0;
}
