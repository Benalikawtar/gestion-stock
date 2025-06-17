# 🗂️ Gestion de Stock – Application Web (Spring Boot)

Ce projet est une application web de gestion des stocks développée dans le cadre du module JEE. Elle permet d’assurer le suivi complet des opérations logistiques liées à des produits répartis dans plusieurs entrepôts.

## 🎯 Objectif

Mettre en place un système centralisé pour gérer les produits, les mouvements de stock (réceptions, livraisons, transferts), les inventaires, ainsi que les entrepôts, avec une interface utilisateur fluide, sécurisée et facile à utiliser.

## 🔍 Fonctionnalités principales

- Authentification des utilisateurs avec sécurité intégrée
- Création, modification et suppression de produits
- Réception des marchandises avec liaison produit–entrepôt
- Livraison des produits depuis les entrepôts
- Transferts entre entrepôts
- Suivi des niveaux de stock par entrepôt
- Réalisation d’inventaires
- Export des données d’inventaire en CSV
- Recherche et filtrage par date ou référence
- Interface web responsive avec Thymeleaf

## 🛠️ Stack technique

- **Backend :** Java 17, Spring Boot, Spring Security, Spring Data JPA
- **Frontend :** Thymeleaf, HTML/CSS
- **Base de données :** MariaDB / MySQL
- **Gestionnaire de dépendances :** Maven

## 🗃️ Structure du projet

- `controller/` : gestion des requêtes utilisateur
- `model/` : entités JPA représentant les données (Produit, Entrepot, etc.)
- `repository/` : interfaces de persistance
- `service/` : logique métier
- `templates/` : vues Thymeleaf

## 📄 Auteur

Projet réalisé par **Kawtar Benali**, dans le cadre du module **Java EE** – 4e année **Génie des Données** – ENSAH.
