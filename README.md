# Dupe Detector

Google Photos can operate as your off-site backup for a proper 3/2/1 backup of your photos and videos. It's also possible
to clone and keep synced your Photos content locally. I set this up for myself and then wanted to delete the copies of
photos that were not part of the local sync of Google Photos (so I wasn't storing 2+ copies of the photos anywhere, and
to figure out which photos I hadn't backed up to Google Photos yet).

There are many file dupe detectors available, but since Photos helps you update the metadata of the photos
(auto-detect location, for example), I wanted to write this to look only at image data for images that are the exact
same, but ignore any metadata (EXIF, etc.) differences.

To speed up subsequent runs, it stores file paths, metadata (last modified date, size), and a hash into an Sqlite
directory. The hash for images is based only on the RGBa data of the image. The hash for videos if of the entire file
because I do not know if there's a way to extract just video data to hash (and this might make the program more
efficient).

Since the hash of a video is based off the entire file's contents, the program only bothers to hash videos if
it finds a video in the source and backup directories that have the same file size.

Comparing file contents is faster than hashing file contents and then comparing the hashes, but I envision this
being run multiple times against the same directories (especially the same Photos sync directory), so storing hashes
and retrieving and comparing via a Sqlite DB is much faster.  

## Usage
`java -jar DupeDetector.jar /path/to/source /path/to/backup (/path/to/destination)`

The software will scan the source and backup directories and then move any dupes in the *source* directory into the
*destination* directory (if there is no *destination* specified, it will move the files to the working directory. 

To solve my problem, *source* would be the directory containing original photos, and *backup*
would be the Google Photos sync directory.