#include <Arduino.h>
#include "FS.h"
#include "SD.h"
#include "SPI.h"

// ─── Pin Definitions for Sunton ESP32-S3 7-inch ───
#define SD_CS   10
#define SD_SCK  12
#define SD_MISO 13
#define SD_MOSI 11

void listDir(fs::FS &fs, const char * dirname, uint8_t levels);
void writeFile(fs::FS &fs, const char * path, const char * message);
void readFile(fs::FS &fs, const char * path);

void setup() {
    // Wait for Serial to be ready (critical for USB CDC)
    Serial.begin(115200);
    delay(2000); 

    Serial.println("\n\n--- Sunton SD Card Test (PlatformIO) ---");

    // 1. Manually configure SPI pins
    SPI.begin(SD_SCK, SD_MISO, SD_MOSI, SD_CS);

    // 2. Mount SD Card
    // Note: We pass the SPI instance and the CS pin
    if (!SD.begin(SD_CS, SPI, 4000000)) {
        Serial.println("❌ Card Mount FAILED");
        Serial.println("   -> Check if card is inserted fully.");
        Serial.println("   -> Check if format is FAT32.");
        return;
    }

    // 1. Create a test file
    writeFile(SD, "/hello.txt", "Hello Sunton Board!");

    // 2. Read it back
    readFile(SD, "/hello.txt");
    
    // 3. List files again to see the new file
    listDir(SD, "/", 0);

    uint8_t cardType = SD.cardType();
    if (cardType == CARD_NONE) {
        Serial.println("❌ No SD card attached");
        return;
    }

    Serial.print("✅ SD Card Detected! Type: ");
    if (cardType == CARD_MMC) Serial.println("MMC");
    else if (cardType == CARD_SD) Serial.println("SDSC");
    else if (cardType == CARD_SDHC) Serial.println("SDHC");
    else Serial.println("UNKNOWN");

    uint64_t cardSize = SD.cardSize() / (1024 * 1024);
    Serial.printf("💾 Size: %llu MB\n", cardSize);

    Serial.println("\n📂 Listing Files:");
    listDir(SD, "/", 0);
}

void loop() {
    delay(1000);
}

// ─── Helper: Recursive File Lister ───
void listDir(fs::FS &fs, const char * dirname, uint8_t levels) {
    Serial.printf("   DIR: %s\n", dirname);

    File root = fs.open(dirname);
    if (!root) {
        Serial.println("    - Failed to open directory");
        return;
    }
    if (!root.isDirectory()) {
        Serial.println("    - Not a directory");
        return;
    }

    File file = root.openNextFile();
    while (file) {
        if (file.isDirectory()) {
            Serial.print("     [DIR]  ");
            Serial.println(file.name());
            if (levels) {
                listDir(fs, file.name(), levels - 1);
            }
        } else {
            Serial.print("     [FILE] ");
            Serial.print(file.name());
            Serial.print("\tSIZE: ");
            Serial.println(file.size());
        }
        file = root.openNextFile();
    }
}

void writeFile(fs::FS &fs, const char * path, const char * message){
    Serial.printf("Writing file: %s\n", path);

    File file = fs.open(path, FILE_WRITE);
    if(!file){
        Serial.println("❌ Failed to open file for writing");
        return;
    }
    if(file.print(message)){
        Serial.println("✅ File written");
    } else {
        Serial.println("❌ Write failed");
    }
    file.close();
}

void readFile(fs::FS &fs, const char * path){
    Serial.printf("Reading file: %s\n", path);

    File file = fs.open(path);
    if(!file){
        Serial.println("❌ Failed to open file for reading");
        return;
    }

    Serial.print("   Read Content: ");
    while(file.available()){
        Serial.write(file.read());
    }
    Serial.println(); // New line
    file.close();
}