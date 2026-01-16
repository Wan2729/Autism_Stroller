#include "Utilities.hpp"

void splitToNumbers(const std::string &s, int &a, int &b, int &c, int &d) {
    size_t p1 = s.find('_');
    size_t p2 = s.find('_', p1 + 1);
    size_t p3 = s.find('_', p2+1);

    a = std::stoi(s.substr(0, p1));
    b = std::stoi(s.substr(p1 + 1, p2 - p1 - 1));
    c = std::stoi(s.substr(p2 + 1, p3 - p2 - 1));
    d = std::stoi(s.substr(p3 + 1));
}