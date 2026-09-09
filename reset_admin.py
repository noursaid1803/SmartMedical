"""
Script pour effacer tous les utilisateurs et créer seulement un admin
"""

from pymongo import MongoClient
import bcrypt

# Connexion à MongoDB
client = MongoClient('mongodb://localhost:27017/')
db = client['test']
users_collection = db['users']

# Effacer tous les utilisateurs
result = users_collection.delete_many({})
print(f"[OK] {result.deleted_count} utilisateurs supprimes de la base")

# Créer l'utilisateur admin
email = "arijhedhri4@gmail.com"
password = "admin123"
role = "ADMIN"

# Hasher le mot de passe avec BCrypt
password_bytes = password.encode('utf-8')
password_hash = bcrypt.hashpw(password_bytes, bcrypt.gensalt()).decode('utf-8')

# Créer l'utilisateur admin
user = {
    'email': email,
    'password': password_hash,
    'role': role,
    'firstName': 'Admin',
    'lastName': 'User',
    'createdAt': None
}
users_collection.insert_one(user)
print(f"[OK] Utilisateur admin cree: {email} / {password}")

print("\n[OK] Base de données réinitialisée avec succès !")
print("Essayez de vous connecter avec:")
print(f"Email: {email}")
print(f"Mot de passe: {password}")
