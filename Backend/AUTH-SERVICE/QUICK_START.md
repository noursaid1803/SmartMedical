# 🚀 Démarrage Rapide - Admin Said Nour

## Option recommandée: Mode Console (Test immédiat, pas de config SMTP)

### Étape 1: Démarrer le backend
```bash
cd Backend/AUTH-SERVICE
mvn spring-boot:run
```

### Étape 2: Créer l'admin Said Nour
Dans un autre terminal PowerShell:
```powershell
curl -X POST http://localhost:8081/admin/setup-said-nour
```

### Étape 3: Récupérer le code dans les logs
Dans la console du service AUTH, vous verrez:
```
========================================
📧 EMAIL DE VÉRIFICATION (MODE CONSOLE)
========================================
À: noour.said1803@gmail.com
Nom: said nour

🎯 CODE DE VÉRIFICATION: 123456

Ce code est valable pendant 24 heures.
========================================
```

**Copiez le code (ex: 123456)**

### Étape 4: Vérifier le compte
```powershell
curl -X POST http://localhost:8081/admin/verify `
  -H "Content-Type: application/json" `
  -d '{"email":"noour.said1803@gmail.com","code":"123456"}'
```

### Étape 5: Se connecter
1. Ouvrez http://localhost:4200/login
2. Email: `noour.said1803@gmail.com`
3. Mot de passe: `Admin123!`

✅ **C'est tout!** Aucune configuration email nécessaire!

---

## Via l'interface Angular

1. Allez sur http://localhost:4200/admin/admin-setup
2. Cliquez sur **"Créer l'admin"**
3. Regardez la console du backend pour le code
4. Saisissez le code dans l'interface
5. Cliquez sur **"Vérifier le code"**

---

## Passer en mode Gmail (Production)

Quand vous voulez envoyer de vrais emails:

### 1. Modifiez `application.properties`:
```properties
# Commentez ceci:
# app.email.mode=console

# Décommentez ceci:
app.email.mode=smtp
spring.mail.username=votre_email@gmail.com
spring.mail.password=votre_app_password
```

### 2. Redémarrez le service
```bash
mvn spring-boot:run
```

---

## 📋 Résumé des commandes

| Action | Commande |
|--------|----------|
| Démarrer le service | `mvn spring-boot:run` |
| Créer Said Nour | `curl -X POST http://localhost:8081/admin/setup-said-nour` |
| Vérifier le code | `curl -X POST http://localhost:8081/admin/verify -d '{"email":"noour.said1803@gmail.com","code":"XXXXXX"}'` |
| Renvoyer le code | `curl -X POST http://localhost:8081/admin/resend-code -d '{"email":"noour.said1803@gmail.com"}'` |

---

## ❓ Problèmes courants

### "Port 8081 already in use"
```bash
# Trouvez et tuez le processus
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### "Cannot connect to MongoDB"
Vérifiez que MongoDB est démarré:
```bash
net start MongoDB
```

### Code non reçu
En mode console, le code s'affiche dans les **logs du backend**, pas par email.

---

**Besoin d'aide?** Consultez `GMAIL_SETUP.md` pour la configuration SMTP complète.
