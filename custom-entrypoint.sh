#!/bin/bash

# Générer keyfile si nécessaire
if [ ! -f /data/db/keyfile ]; then
  openssl rand -base64 756 > /data/db/keyfile
  chmod 400 /data/db/keyfile
  chown 999:999 /data/db/keyfile
fi

# Démarrer MongoDB SANS authentification
echo "Démarrage de MongoDB sans authentification..."
mongod --replSet rs0 --bind_ip_all --fork --logpath /var/log/mongodb.log

# Attendre que MongoDB soit prêt
echo "Attente du démarrage de MongoDB..."
until mongosh --host localhost --eval "db.adminCommand({ ping: 1 })"
do
  echo "Attente de la disponibilité de MongoDB..."
  sleep 2
done

# Initialiser le replica set
mongosh --host localhost --eval "rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'localhost:27017'}]})"

# Créer l'utilisateur admin
mongosh --host localhost --eval "
  db = db.getSiblingDB('admin');
  db.createUser({
    user: 'admin',
    pwd: 'admin',
    roles: ['root']
  });"

# Exécuter les scripts d'initialisation .js
for f in /docker-entrypoint-initdb.d/*.js; do
  echo "Exécution du script $f"
  mongosh --host localhost -u admin -p admin --authenticationDatabase admin "$f"
done

# Arrêter MongoDB
mongosh --host localhost --eval "db.adminCommand({shutdown: 1})"
sleep 3

# Redémarrer avec authentification
echo "Redémarrage de MongoDB avec authentification..."
exec mongod --replSet rs0 --bind_ip_all --keyFile /data/db/keyfile