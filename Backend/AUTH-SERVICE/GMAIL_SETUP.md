# Configuration Gmail SMTP - Guide Complet

## Option 1: Configuration Gmail (Production)

### Étape 1: Créer le compte Gmail (si pas déjà fait)
1. Allez sur https://accounts.google.com/signup
2. Créez un compte avec :
   - **Email** : smartmedical.app@gmail.com
   - **Mot de passe** : Choisissez un mot de passe sécurisé

### Étape 2: Activer la Vérification en 2 étapes (OBLIGATOIRE)
1. Connectez-vous à https://myaccount.google.com
2. Allez dans **Sécurité** dans le menu de gauche
3. Cliquez sur **Vérification en deux étapes**
4. Suivez les étapes :
   - Confirmez votre téléphone
   - Choisissez **"Application d'authentification"** ou **SMS**
   - Validez le code reçu

### Étape 3: Générer un App Password
1. Toujours dans **Sécurité** > **Vérification en deux étapes**
2. Descendez à **"Mots de passe des applications"**
3. Cliquez sur **"Sélectionner l'application"** → Choisissez **"Autre (nom personnalisé)"**
4. Nommez-le : **SmartMedical**
5. Cliquez sur **GÉNÉRER**
6. **COPIEZ le code à 16 caractères** (ex: abcd efgh ijkl mnop)

### Étape 4: Configurer le backend

**Méthode A: Variable d'environnement (Recommandée)**
```powershell
# Windows PowerShell
$env:GMAIL_APP_PASSWORD = "abcd efgh ijkl mnop"
# Puis démarrez le service
mvn spring-boot:run
```

**Méthode B: Directement dans le fichier**

Ouvrez `src/main/resources/application.properties` :
```properties
spring.mail.username=smartmedical.app@gmail.com
spring.mail.password=abcd efgh ijkl mnop
```

⚠️ **NE JAMAIS commiter ce fichier avec le vrai mot de passe sur Git!**

---

## Option 2: Mode Développement (Console) - RECOMMANDÉ POUR TESTS

Si vous ne voulez pas configurer Gmail tout de suite, utilisez le mode développement qui affiche le code dans la console.

### Avantage
- Pas besoin de configuration email
- Le code s'affiche directement dans les logs
- Parfait pour les tests en développement

### Désavantage
- L'email n'est pas vraiment envoyé
- Le destinataire ne reçoit rien

### Comment l'activer

Dans `application.properties`, changez :
```properties
app.email.mode=console
```

Au lieu de :
```properties
app.email.mode=smtp
```

---

## Option 3: Utiliser un SMTP de test (Mailtrap)

Pour les tests sans envoyer de vrais emails :

1. Créez un compte sur https://mailtrap.io
2. Allez dans **Inbox** → **SMTP Settings**
3. Choisissez **Spring Boot** dans le dropdown
4. Copiez les credentials

Modifiez `application.properties` :
```properties
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=VOTRE_USERNAME_MAILTRAP
spring.mail.password=VOTRE_PASSWORD_MAILTRAP
```

Les emails seront capturés dans l'interface Mailtrap au lieu d'être envoyés.

---

## Vérification

Pour vérifier que tout fonctionne :

1. Démarrez le service AUTH :
```bash
cd Backend/AUTH-SERVICE
mvn spring-boot:run
```

2. Testez avec cURL :
```bash
curl -X POST http://localhost:8081/admin/setup-said-nour
```

3. Vérifiez les logs :
- Si mode SMTP : "Email de vérification envoyé à : noour.said1803@gmail.com"
- Si mode Console : Le code à 6 chiffres s'affiche dans la console

---

## Dépannage

### "Authentication failed"
- Vérifiez que vous utilisez un **App Password**, pas votre mot de passe Gmail normal
- Vérifiez que la vérification 2 étapes est bien activée

### "Connection refused"
- Vérifiez votre connexion internet
- Essayez avec un VPN si vous êtes dans un réseau restreint

### "Bad credentials"
- Régénérez un nouvel App Password
- L'ancien a peut-être été révoqué

---

## Besoin d'aide ?

Si vous bloquez, je peux :
1. Configurer le mode console pour vos tests immédiats
2. Créer un script de configuration automatique
3. Passer en visio pour vous guider étape par étape

**Recommandation** : Commencez par le **mode Console** pour tester la fonctionnalité, puis configurez Gmail quand tout fonctionne.
