import glob
import os
import re
import sqlite3

import numpy as np
import progressbar
from bs4 import BeautifulSoup
from gensim.models.doc2vec import Doc2Vec, TaggedDocument


def getImageLink(content):
    # Parse the HTML content
    soup = BeautifulSoup(content, "html.parser")

    # Find the img tag
    img_tag = soup.find("img")

    # Extract the src attribute
    if img_tag:
        image_src = img_tag.get("src")
        return image_src
    else:
        return None


def createDB(verbose=False):
    # Delete old databases
    if os.path.exists("models/src/main/assets/databases/monuments_db.sqlite"):
        os.remove("models/src/main/assets/databases/monuments_db.sqlite")

    # Get languages from names of folders in currentGuide folder
    languages = [
        name
        for name in os.listdir("models/src/main/assets/currentGuide/Template Monument")
        if os.path.isdir(
            os.path.join("models/src/main/assets/currentGuide/Template Monument", name)
        )
    ]
    if verbose:
        print("Language found: ", languages)
    if len(languages) == 0:
        print("[ERROR] No languages found in currentGuide folder")
        exit()

    # Create a new database
    con = sqlite3.connect("models/src/main/assets/databases/monuments_db.sqlite")
    cur = con.cursor()
    cur.execute("DROP TABLE IF EXISTS Languages")
    cur.execute(
        """ CREATE TABLE Languages (id INTEGER PRIMARY KEY AUTOINCREMENT, language) """
    )
    for i, lang in enumerate(languages):
        sql = """ INSERT INTO Languages (language)
                    VALUES(?) """
        cur.execute(sql, (lang,))
        con.commit()

    for lang in languages:
        if verbose:
            print("\n\nCreating DB for " + lang + " language...")

        # GETTING DB NAMES
        db_name = f"monuments_db_{lang}.sqlite"
        table_name_monuments = f"monuments_{lang}"
        table_name_attributes = f"attributes_{lang}"
        table_name_categories = f"categories_{lang}"
        table_name_monuments_categories = f"monuments_categories_{lang}"
        table_name_monuments_attributes = f"monuments_attributes_{lang}"

        # GETTING PATHS OF GUIDE FILES
        path = f"models/src/main/assets/currentGuide/*/{lang}/guide.md"
        textPaths = glob.glob(path)

        if verbose:
            print(
                "Path used for currentGuide: "
                + path
                + "\n"
                + "Number of files found: "
                + str(len(textPaths))
            )
        if len(textPaths) == 0:
            print(
                "No files found in "
                + path
                + "\n"
                + "Please check the path and try again"
            )
            exit()

        monumentsList = []
        attributesList = []
        categoriesList = []

        # REMOVING TEMPLATE MONUMENT FROM LIST
        for i in range(len(textPaths)):
            pathSplitted = textPaths[i].split(os.path.sep)[-3].split(" ")
            m = " ".join([str(elem) for elem in pathSplitted])

            if m == "Template Monument":
                textPaths.pop(i)
                break

        # PARSING GUIDE FILES
        for i, path in enumerate(textPaths):
            # Open text file in read mode
            file = open(textPaths[i], "r", encoding="utf-8")

            # Read whole file to a string
            content = file.read()

            # Split the content into lines
            lines = content.split("\n")

            # COORDINATES (Second line of the file)
            if lines[1] != "":
                # Split using spaces or commas
                coordinates = re.split(r"\s|,\s*", lines[1])
            else:
                coordinates = ("null", "null")
                print(f"[WARNING] No coordinates found for guide {textPaths[i]}")

            # CATEGORIES (Third line of the file)
            capitalized_categories = []
            if lines[2] != "":
                categories = lines[2].split(",")

                # Uppercase the first letter of each string
                temp_capitalized_categories = [s.strip().title() for s in categories]

                # Add to attributes list each capitalized attribute if not already present
                for cat in temp_capitalized_categories:
                    if cat not in categoriesList:
                        categoriesList.append(cat)
                    if cat not in capitalized_categories:
                        capitalized_categories.append(cat)
            else:
                print(f"[WARNING] No categories found for guide {textPaths[i]}")

            # ATTRIBUTES (Fourth line of the file)
            capitalized_attributes = []
            if lines[3] != "":
                attributes = lines[3].split(",")

                # Uppercase the first letter of each string
                temp_capitalized_attributes = [s.strip().title() for s in attributes]

                # Add to attributes list each capitalized attribute if not already present
                for attr in temp_capitalized_attributes:
                    if attr not in attributesList:
                        attributesList.append(attr)
                    if attr not in capitalized_attributes:
                        capitalized_attributes.append(attr)
            else:
                print(f"[WARNING] No attributes found for guide {textPaths[i]}")

            # SUBTITLE (Fifth line of the file)
            subtitle = lines[4]
            if subtitle == "" or subtitle == "-->":
                subtitle = ""
                print(f"[WARNING] No subtitle found for guide {textPaths[i]}")

            # LINK WEB PAGE (Sixth line of the file)
            url = lines[5]
            if url == "" or url == "-->":
                url = ""
                print(f"[WARNING] No URL found for guide {textPaths[i]}")

            # Check if the categories have the corresponding image
            for cat in capitalized_categories:
                if not os.path.exists(
                    f"models/src/main/assets/currentCategories/{cat}.jpg"
                ):
                    print(
                        f"[WARNING] Image not found for category {cat} in guide {textPaths[i]}"
                    )

            # Create a tuple with the content, coordinates, categories, attributes and image link
            obj = (
                content,
                coordinates,
                capitalized_categories,
                capitalized_attributes,
                subtitle,
                url,
            )

            monumentsList.append(obj)

            # close file
            file.close()

        # CREATING DOC2VEC MODEL
        documents = [TaggedDocument(doc[0], [i]) for i, doc in enumerate(monumentsList)]
        model = Doc2Vec(
            documents, vector_size=100, window=5, min_count=1, workers=4, epochs=20
        )
        desc_vectors = np.zeros((len(monumentsList), model.vector_size))
        for i in range(len(monumentsList)):
            desc_vectors[i] = model.dv[i]

        # CREATING SQL LITE DATABASE FOR MONUMENTS
        # con = sqlite3.connect("../models/src/main/assets/databases/monuments_db.sqlite")

        # CATEGORIES
        cur = con.cursor()

        cur.execute(f"DROP TABLE IF EXISTS {table_name_categories}")
        cur.execute(
            f""" CREATE TABLE {table_name_categories} (id INTEGER PRIMARY KEY AUTOINCREMENT, name) """
        )

        if verbose:
            widgets = [
                "[INFO]: Saving database (Categories - " + lang + ") ... ",
                progressbar.Percentage(),
                " ",
                progressbar.Bar(),
                " ",
                progressbar.ETA(),
            ]
            pbar = progressbar.ProgressBar(
                maxval=len(categoriesList), widgets=widgets
            ).start()

        for i, attr in enumerate(categoriesList):
            sql = f""" INSERT INTO {table_name_categories} (name)
                    VALUES(?) """
            cur.execute(sql, (attr,))
            con.commit()
            if verbose:
                pbar.update(i)
        if verbose:
            pbar.finish()

        # ATTRIBUTES
        cur = con.cursor()

        cur.execute(f"DROP TABLE IF EXISTS {table_name_attributes}")
        cur.execute(
            f""" CREATE TABLE {table_name_attributes} (id INTEGER PRIMARY KEY AUTOINCREMENT, name) """
        )

        if verbose:
            widgets = [
                "[INFO]: Saving database (Attributes - " + lang + ") ... ",
                progressbar.Percentage(),
                " ",
                progressbar.Bar(),
                " ",
                progressbar.ETA(),
            ]
            pbar = progressbar.ProgressBar(
                maxval=len(attributesList), widgets=widgets
            ).start()

        for i, attr in enumerate(attributesList):
            sql = f""" INSERT INTO {table_name_attributes} (name)
                    VALUES(?) """
            cur.execute(sql, (attr,))
            con.commit()
            if verbose:
                pbar.update(i)

        if verbose:
            pbar.finish()

        # RELATIONS BETWEEN MONUMENTS AND CATEGORIES  monumentsList[i][2], str(monumentsList[i][3])
        cur = con.cursor()

        cur.execute(f"DROP TABLE IF EXISTS {table_name_monuments_categories}")
        cur.execute(f""" CREATE TABLE {table_name_monuments_categories} (
                    monumentID INTEGER,
                    categoryID INTEGER,
                    PRIMARY KEY (monumentID, categoryID),
                    FOREIGN KEY (monumentID) REFERENCES {table_name_monuments}(id),
                    FOREIGN KEY (categoryID) REFERENCES {table_name_categories}(id)) """)

        # RELATIONS BETWEEN MONUMENTS AND ATTRIBUTES
        cur = con.cursor()

        cur.execute(f"DROP TABLE IF EXISTS {table_name_monuments_attributes}")
        cur.execute(f""" CREATE TABLE {table_name_monuments_attributes} (
                    monumentID INTEGER,
                    attributeID INTEGER,
                    PRIMARY KEY (monumentID, attributeID),
                    FOREIGN KEY (monumentID) REFERENCES {table_name_monuments}(id),
                    FOREIGN KEY (attributeID) REFERENCES {table_name_attributes}(id)) """)

        # MONUMENTS
        cur = con.cursor()

        cur.execute(f"DROP TABLE IF EXISTS {table_name_monuments}")
        cur.execute(
            f""" CREATE TABLE {table_name_monuments} (id INTEGER PRIMARY KEY AUTOINCREMENT, monument, vec, coordX, coordY, subtitle, url) """
        )

        if verbose:
            widgets = [
                "[INFO]: Saving database (Monuments - " + lang + ") ... ",
                progressbar.Percentage(),
                " ",
                progressbar.Bar(),
                " ",
                progressbar.ETA(),
            ]
            pbar = progressbar.ProgressBar(
                maxval=len(desc_vectors), widgets=widgets
            ).start()

        for i, v in enumerate(desc_vectors):
            # Insert a row of data
            val = str(v)
            pathSplitted = textPaths[i].split(os.path.sep)[-3].split(" ")
            m = " ".join([str(elem) for elem in pathSplitted])

            sql = f""" INSERT INTO {table_name_monuments} (monument, vec, coordX, coordY, subtitle, url)
                    VALUES(?,?,?,?,?,?) """

            new = cur.execute(
                sql,
                (
                    m,
                    val,
                    monumentsList[i][1][0],
                    monumentsList[i][1][1],
                    monumentsList[i][4],
                    monumentsList[i][5],
                ),
            )
            # print(f"Monument: {m}, # cat.: {len(monumentsList[i][2])}, # attr.: {len(monumentsList[i][3])}")

            # Save (commit) the changes
            con.commit()

            lastID = new.lastrowid

            for cat in monumentsList[i][2]:
                try:
                    sql = f""" INSERT INTO {table_name_monuments_categories} (monumentID, categoryID)
                            VALUES(?,?) """
                    cur.execute(sql, (lastID, categoriesList.index(cat) + 1))
                    con.commit()
                except sqlite3.IntegrityError as e:
                    con.rollback()  # Rollback the transaction to avoid partially committed data
                    print(
                        f"IntegrityError: {e} - MonumentID: {lastID}, CategoryID: {categoriesList.index(cat) + 1} - Monument: {m}, Category: {cat}"
                    )

            for attr in monumentsList[i][3]:
                try:
                    sql = f""" INSERT INTO {table_name_monuments_attributes} (monumentID, attributeID)
                                VALUES(?,?) """
                    cur.execute(sql, (lastID, attributesList.index(attr) + 1))
                    con.commit()
                except sqlite3.IntegrityError as e:
                    con.rollback()  # Rollback the transaction to avoid partially committed data
                    print(
                        f"IntegrityError: {e} - MonumentID: {lastID}, AttributeID: {attributesList.index(attr) + 1} - Monument: {m}, Attribute: {attr}"
                    )

            if verbose:
                pbar.update(i)

        if verbose:
            pbar.finish()

        # Close the connection
    con.close()

    if verbose:
        print(
            "\n\nDatabases saved in "
            + os.path.realpath("models/src/main/assets/databases")
        )


# MAIN FUNCTION
if __name__ == "__main__":
    createDB()
