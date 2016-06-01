#include "syscall.h"
#include "stdlib.h"
int main() {
   int fdA = open("USConst.txt");
   int fdB = open("GuyFawkes.txt");
   printf("fdA: %d; Supposed to be 2;\n",fdA);
   printf("fdB: %d; Supposed to be 3;\n",fdB);

   int status = close(2);     // Normal close
   printf("closeStatus: %d; Supposed to be 0;\n",status);

   status = close(2);         // Close a already closed file
   printf("closeStatus: %d; Supposed to be -1;\n",status);

   fdA = open("0Char.txt");   // Check if free fd is reused
   printf("fdA: %d; Supposed to be 2;\n",fdA);

   halt();
   return 0;
}
