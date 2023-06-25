import numpy as np
import pandas as pd
from gensim.models.doc2vec import Doc2Vec, TaggedDocument
import glob
import sqlite3
import progressbar
import os
import argparse

languages = ['English', 'Italian']  # Add more languages as needed



def createDB():

    for lang in languages:

        print("\n\nCreating DB for " + lang + " language...")

        #GETTING DB NAMES
        db_name = f"monuments_db_{lang}.sqlite"
        table_name_monuments = f"monuments_{lang}"
        table_name_attributes = f"attributes_{lang}"
        table_name_categories = f"categories_{lang}"

        #GETTING PATHS OF GUIDE FILES
        path = f"../models/src/main/assets/guides/*/{lang}/guide.md"
        textPaths = glob.glob(path)

        print("Path used for guides: " + path + '\n' + "Number of files found: " + str(len(textPaths)))
        if(len(textPaths)==0):
            print("No files found in " + path + '\n' + "Please check the path and try again")
            exit()

        monumentsList = []
        attributesList = []
        categoriesList = []

        #REMOVING TEMPLATE MONUMENT FROM LIST
        for i in range(len(textPaths)):
            pathSplitted = textPaths[i].split(os.path.sep)[-3].split(' ')
            m = ' '.join([str(elem) for elem in pathSplitted])

            if (m == "Template Monument"):
                textPaths.pop(i)
                break

        #READING GUIDE FILES
        for i, path in enumerate(textPaths):
            
            #open text file in read mode
            file = open(textPaths[i], "r", encoding="utf-8")
            
            #Read whole file to a string
            content = file.read()

            # Split the content into lines
            lines = content.split('\n')

            #COORDINATES
            if lines[1] != '':
                coordinates = lines[1].split()
            else:
                coordinates = ("null", "null")


            #CATEGORIES
            if lines[2] != '':
                categories = lines[2].split(',')

                # Uppercase the first letter of each string
                capitalized_categories = [s.strip().title() for s in categories]

                #Add to attrivutes list each capitalized attribute if not already present
                for attr in capitalized_categories:
                    if attr not in categoriesList:
                        categoriesList.append(attr)

                # Convert the capitalized strings list to a single string
                capitalized_categories_as_string = ', '.join(capitalized_categories)
            else:
                capitalized_categories_as_string = "null"



            #ATTRIBUTES
            if lines[3] != '':
                attributes = lines[3].split(',')

                # Uppercase the first letter of each string
                capitalized_attributes = [s.strip().title() for s in attributes]

                #Add to attrivutes list each capitalized attribute if not already present
                for attr in capitalized_attributes:
                    if attr not in attributesList:
                        attributesList.append(attr)

                # Convert the capitalized strings list to a single string
                capitalized_attributes_as_string = ', '.join(capitalized_attributes)
            else:
                capitalized_attributes_as_string = "null"

            

            # Create a tuple with the content, coordinates, categories and attributes
            obj = (content, coordinates, capitalized_categories_as_string, capitalized_attributes_as_string)

            monumentsList.append(obj)
            
            #close file
            file.close()

        #CREATING DOC2VEC MODEL
        documents = [TaggedDocument(doc[0], [i]) for i, doc in enumerate(monumentsList)] 
        model = Doc2Vec(documents, vector_size=100, window=5, min_count=1, workers=4, epochs=20)
        desc_vectors = np.zeros((len(monumentsList), model.vector_size))
        for i in range(len(monumentsList)):
            desc_vectors[i] = model.dv[i]


        #CREATING SQL LITE DATABASE FOR MONUMENTS

        con = sqlite3.connect("../models/src/main/assets/databases/monuments_db.sqlite")
        cur = con.cursor()

        cur.execute(f"DROP TABLE IF EXISTS {table_name_monuments}")
        cur.execute(f""" CREATE TABLE {table_name_monuments} (id INTEGER PRIMARY KEY AUTOINCREMENT, monument, vec, coordX, coordY, categories, attributes) """)

        widgets = ["[INFO]: Saving database (Monuments - " + lang + ") ... ", progressbar.Percentage(), " ", progressbar.Bar(), " ", progressbar.ETA()]

        pbar = progressbar.ProgressBar(maxval=len(desc_vectors), widgets=widgets).start()

        for i, v in enumerate(desc_vectors):
            # Insert a row of data
            val = str(v)
            pathSplitted = textPaths[i].split(os.path.sep)[-3].split(' ')
            m = ' '.join([str(elem) for elem in pathSplitted])

            sql = f''' INSERT INTO {table_name_monuments} (monument, vec, coordX, coordY, categories, attributes)
                    VALUES(?,?,?,?,?,?) '''
            
            new = cur.execute(sql, (m,val, monumentsList[i][1][0], monumentsList[i][1][1], str(monumentsList[i][2]), str(monumentsList[i][3])))

            # Save (commit) the changes
            con.commit()
            pbar.update(i)

        pbar.finish()
        con.close()

        #Save the following in another file

        #CREATING SQL LITE DATABASE FOR CATEGORIES

        con = sqlite3.connect("../models/src/main/assets/databases/list_of_attributes_categories_db.sqlite")
        cur = con.cursor()

        cur.execute(f"DROP TABLE IF EXISTS {table_name_categories}")
        cur.execute(f""" CREATE TABLE {table_name_categories} (id INTEGER PRIMARY KEY AUTOINCREMENT, name) """)


        widgets = ["[INFO]: Saving database (Categories - " + lang + ") ... ", progressbar.Percentage(), " ", progressbar.Bar(), " ", progressbar.ETA()]

        pbar = progressbar.ProgressBar(maxval=len(categoriesList), widgets=widgets).start()

        for i, attr in enumerate(categoriesList):
            sql = f''' INSERT INTO {table_name_categories} (name)
                    VALUES(?) '''
            cur.execute(sql, (attr,))
            con.commit()
            pbar.update(i)

        pbar.finish()

        #CREATING SQL LITE DATABASE FOR ATTRIBUTES

        cur.execute(f"DROP TABLE IF EXISTS {table_name_attributes}")
        cur.execute(f""" CREATE TABLE {table_name_attributes} (id INTEGER PRIMARY KEY AUTOINCREMENT, name) """)


        widgets = ["[INFO]: Saving database (Attributes - " + lang + ") ... ", progressbar.Percentage(), " ", progressbar.Bar(), " ", progressbar.ETA()]
        
        pbar = progressbar.ProgressBar(maxval=len(attributesList), widgets=widgets).start()

        for i, attr in enumerate(attributesList):
            print("\n",attr)
            sql = f''' INSERT INTO {table_name_attributes} (name)
                    VALUES(?) '''
            cur.execute(sql, (attr,))
            con.commit()
            pbar.update(i)

        # Close the connection
        con.close()
        pbar.finish()

    print("\n\nDatabases saved in " + os.path.realpath('../models/src/main/assets/databases'))





#MAIN FUNCTION
if __name__ == "__main__":
    createDB()