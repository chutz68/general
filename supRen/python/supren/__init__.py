"""supren - modern replacement for the Java supRen photo-renaming tool.

Three functions, matching the legacy tests/scripts:

- rename   EXIF-camera + date renaming (RenameNewPhotosMetaDataExtractorTest)
- bestof   copy rated files into a BestOf tree (CopyBestOfMetaDataExtractorTest,
           scripts/synology/bestof_copy.sh)
- unrated  find unrated files inside a BestOf tree (FindBestOfWithoutRatingTest)
- cleanup  clean up sync-conflict duplicates (scripts/synology/cleanup_conflicts.sh)

See python/README.md for usage.
"""
