#!/bin/bash
# 01-init_replica.sh

#!/bin/bash

# Attendre que MongoDB soit prêt
echo "Attente du démarrage de MongoDB..."
until mongosh --host localhost --eval "db.adminCommand({ ping: 1 })"
do
  echo "Attente de la disponibilité de MongoDB..."
  sleep 2
done

# Initialiser le replica set (sans authentification)
mongosh --host localhost --eval "
  rs.initiate({
    _id: 'rs0',
    members: [{_id: 0, host: 'localhost:27017'}]
  });"

# Attendre que le replica set soit initialisé
sleep 5

# Créer l'utilisateur admin
mongosh --host localhost --eval "
  db = db.getSiblingDB('admin');
  if (!db.getUser('admin')) {
    db.createUser({
      user: 'admin',
      pwd: 'admin',
      roles: ['root']
    });
    print('Utilisateur admin créé');
  } else {
    print('Utilisateur admin existe déjà');
  }"

echo "Replica set et utilisateur admin initialisés!"

