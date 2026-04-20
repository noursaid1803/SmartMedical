# Configuration Admin - Said Nour

Ce document explique comment créer et configurer le compte admin **said nour** avec vérification par email.

## 📧 Email configuré
- **Nom** : said nour
- **Email** : noour.said1803@gmail.com
- **Mot de passe temporaire** : Admin123!

## 🚀 Étapes rapides

### 1. Configurer l'envoi d'email (Gmail SMTP)

Pour que le service d'email fonctionne, vous devez configurer un **App Password** Gmail :

1. Allez sur https://myaccount.google.com/
2. Activez la **Vérification en deux étapes** (obligatoire pour les App Passwords)
3. Allez dans **Sécurité** > **Mots de passe des applications**
4. Créez un nouveau mot de passe pour "Autre (nom personnalisé)" → nommez-le "SmartMedical"
5. Copiez le code généré (16 caractères sans espaces)

### 2. Configurer les variables d'environnement

Option A - Via ligne de commande (Windows PowerShell) :
```powershell
$env:GMAIL_APP_PASSWORD = "votre_app_password_16_caracteres"
```

Option B - Directement dans `application.properties` :
```properties
spring.mail.password=votre_app_password_16_caracteres
```

Option C - Via IntelliJ IDEA :
- Run > Edit Configurations > Environment variables
- Ajoutez : `GMAIL_APP_PASSWORD=votre_app_password_16_caracteres`

### 3. Démarrer le service AUTH

```bash
cd Backend/AUTH-SERVICE
mvn spring-boot:run
```

### 4. Créer l'admin Said Nour

#### Option A - Via l'interface Angular (recommandé)
1. Ouvrez l'application Angular : http://localhost:4200
2. Connectez-vous en tant qu'admin existant
3. Allez dans **Admin** > **Nouvel Admin**
4. Cliquez sur **"Créer l'admin"** pour said nour

#### Option B - Via API REST (curl/Postman)
```bash
curl -X POST http://localhost:8081/admin/setup-said-nour \
  -H "Content-Type: application/json"
```

#### Option C - Créer un admin personnalisé
```bash
curl -X POST http://localhost:8081/admin/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "said nour",
    "email": "noour.said1803@gmail.com",
    "password": "Admin123!"
  }'
```

### 5. Vérifier le compte

Après création, l'admin recevra un email avec un **code de vérification à 6 chiffres**.

#### Via API :
```bash
curl -X POST http://localhost:8081/admin/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "noour.said1803@gmail.com",
    "code": "123456"
  }'
```

#### Via Angular :
- Allez sur la page **"Nouvel Admin"**
- Saisissez le code reçu par email
- Cliquez sur **"Vérifier le code"**

## 🔌 Endpoints API disponibles

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/admin/setup-said-nour` | POST | Crée directement l'admin said nour |
| `/admin/create` | POST | Crée un admin personnalisé |
| `/admin/verify` | POST | Vérifie le code et active le compte |
| `/admin/resend-code` | POST | Renvoie un nouveau code |

## 📋 Exemples de requêtes

### Créer un admin
```json
POST http://localhost:8081/admin/create
{
  "name": "said nour",
  "email": "noour.said1803@gmail.com",
  "password": "Admin123!"
}
```

Réponse :
```json
{
  "success": true,
  "message": "Compte admin créé avec succès. Un code de vérification a été envoyé...",
  "adminId": "...",
  "email": "noour.said1803@gmail.com",
  "requiresVerification": true
}
```

### Vérifier le code
```json
POST http://localhost:8081/admin/verify
{
  "email": "noour.said1803@gmail.com",
  "code": "123456"
}
```

Réponse :
```json
{
  "success": true,
  "message": "Compte vérifié et activé avec succès !",
  "adminId": "...",
  "email": "noour.said1803@gmail.com",
  "isVerified": true
}
```

## ⚠️ Notes importantes

1. **Le code de vérification expire après 24 heures**
2. **Utilisez toujours un App Password Gmail**, jamais votre mot de passe normal
3. **Le compte doit être vérifié avant de pouvoir se connecter**
4. **Changez le mot de passe après la première connexion**

## 🔧 Dépannage

### Problème : Email non reçu
- Vérifiez vos spams/pourriels
- Cliquez sur "Renvoyer le code"
- Vérifiez que l'App Password est correctement configuré

### Problème : "Erreur d'authentification SMTP"
- Vérifiez que la vérification en deux étapes est activée sur Gmail
- Régénérez un nouvel App Password
- Vérifiez que la variable d'environnement est bien définie

### Problème : "Un utilisateur avec cet email existe déjà"
- L'admin a déjà été créé
- Utilisez la fonction "Renvoyer le code" pour obtenir un nouveau code

## 🔒 Sécurité

- Les mots de passe sont hashés avec BCrypt
- Les codes de vérification sont uniques et expirent après 24h
- L'activation par email empêche les créations de comptes frauduleuses
- Utilisez HTTPS en production pour sécuriser les communications
