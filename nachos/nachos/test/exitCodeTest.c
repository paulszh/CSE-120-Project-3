#include "syscall.h"

int main(int argc, char *agrv[]) {
    int pid = exec("exitCode100Test.coff", 0, 0);
    int status;
    int ret = join(pid, &status);

    printf("return value = %d, should be 1\n", ret);
    printf("status = %d, shoule be 100\n", status);

    pid = exec("unhandledException.coff", 0, 0);
    ret = join(pid, &status);

    printf("return value = %d, should be 0\n", ret);
    printf("status is undefined\n");

    ret = join(10000, &status);
    printf("return value = %d, should be -1\n", ret);

    exit(0);
}
