#include "syscall.h"
int main() {
   int readBytes;
   int totalReadBytes = 0;
   char buffer[1024];
   int fd = open("GuyFawkes.txt");
   int badFd = open("GuyFAWKES.txt");
   int protFd = open("R0W0.txt");

   readBytes = read(-1, buffer, 1024);
   if (readBytes == -1) {
      printf("Bad fd: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad fd: Fatal Error Not Encountered; Unexpected;\n");
   }

   readBytes = read(1024, buffer, 1024);
   if (readBytes == -1) {
      printf("Bad fd: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad fd: Fatal Error Not Encountered; Unexpected;\n");
   }


   readBytes = read(badFd, buffer, 1024);
   if (readBytes == -1) {
      printf("Bad fd: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad fd: Fatal Error Not Encountered; Unexpected;\n");
   }

   readBytes = read(fd, buffer, -1);
   if (readBytes == -1) {
      printf("Bad num of byte: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad num of byte: Fatal Error Not Encountered; Unexpected;\n");
   }

   readBytes = read(fd, -1, 1024); 
   if (readBytes == -1) {
      printf("Bad Buffer: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad Buffer: Fatal Error Not Encountered; Unexpected;\n");
   }

   readBytes = read(protFd, buffer, 1024); 
   if (readBytes == -1) {
      printf("Can't Read File: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Can't Read File: Fatal Error Not Encountered; Unexpected;\n");
   }

   halt();
   return 0;
}
