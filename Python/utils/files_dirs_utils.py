import os

def has_trailing_spaces(directory_path):
    # Check if the last character is a space
    return directory_path.rstrip() != directory_path

# Function to remove trailing spaces from a directory path
def remove_and_rename_trailing_spaces_dir(directory_path):
    if os.path.isdir(directory_path):
        # Check if the last character is a space
        if has_trailing_spaces(directory_path):
            # Remove trailing spaces
            cleaned_path = directory_path.rstrip()
            # Rename the directory
            print(f"[WARN] Renaming {directory_path} to {cleaned_path}")
            os.rename(directory_path, cleaned_path)
            return cleaned_path
        else:
            return directory_path
