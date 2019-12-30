SELECT source_files.id, source_files.absolute_path, source_files.hash_id,  backup_files.hash_id, backup_files.absolute_path
    FROM source_files
    INNER JOIN hash_list
        ON source_files.hash_id = hash_list.id
    INNER JOIN backup_files
        ON backup_files.hash_id = hash_list.id
    WHERE source_files.session_id = 123 AND backup_files.session_id = 123
