"""
Script pour créer un utilisateur admin dans MongoDB avec BCrypt
"""

from pymongo import MongoClient
import bcrypt

# Connexion à MongoDB
client = MongoClient('mongodb://localhost:27017/')
db = client['test']
users_collection = db['users']

# Créer l'utilisateur admin
email = "arijhedhri4@gmail.com"
password = "admin123"
role = "ADMIN"

# Hasher le mot de passe avec BCrypt
password_bytes = password.encode('utf-8')
password_hash = bcrypt.hashpw(password_bytes, bcrypt.gensalt()).decode('utf-8')

# Vérifier si l'utilisateur existe déjà
existing_user = users_collection.find_one({'email': email})
if existing_user:
    print(f"Utilisateur {email} existe déjà.")
    print(f"Rôle actuel: {existing_user.get('role', 'N/A')}")
    
    # Mettre à jour le mot de passe
    users_collection.update_one(
        {'email': email},
        {'$set': {'password': password_hash, 'role': role}}
    )
    print(f"Mot de passe mis à jour avec BCrypt et rôle défini à {role}")
else:
    # Créer l'utilisateur
    user = {
        'email': email,
        'password': password_hash,
        'role': role,
        'firstName': 'Admin',
        'lastName': 'User',
        'createdAt': None
    }
    users_collection.insert_one(user)
    print(f"Utilisateur admin créé avec BCrypt: {email} / {password}")

print("\nEssayez de vous connecter avec:")
print(f"Email: {email}")
print(f"Mot de passe: {password}")
