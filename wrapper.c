#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

extern int64_t func(int64_t);

int main(int argc, char *argv[]) {
    int64_t inp, out;
    scanf("%lld", &inp);
    out = func(inp);
    printf("%ld\n", out);
    return EXIT_SUCCESS;
}