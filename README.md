# CampusRoom — Réservation de salles par système multi-agents

Projet pédagogique du module **Intelligence Artificielle et Systèmes Multi-Agents**, Master IAA.


## 1. Ce que fait l'application

Vous saisissez une date, une heure de début, une heure de fin, un effectif et le besoin d'un vidéoprojecteur. Un agent demandeur contacte un coordinateur. Le coordinateur découvre les salles dans le DF et les interroge. Chaque salle consulte **son propre calendrier** et propose ou refuse. Le coordinateur choisit la salle compatible avec le moins de places inutilisées. La réservation n'est confirmée qu'après le dernier contrôle de la salle.

Salles initiales :

| Agent | Salle | Capacité | Vidéoprojecteur |
|---|---|---:|---|
| salleA | Salle A | 20 | Oui |
| salleB | Salle B | 35 | Oui |
| salleC | Salle C | 60 | Non |

Trois résultats sont possibles :
- `CONFIRMEE` : la salle a enregistré la réservation et a répondu.
- `ECHEC` : aucune réservation n'a été confirmée pour cette demande ; lire la raison.
- `INCERTAINE` : une réservation peut avoir été enregistrée, mais la confirmation manque. **Vérifier avant de soumettre à nouveau.** Le programme ne réserve pas automatiquement une deuxième salle.

Chaque clic crée une NOUVELLE demande. Ce n'est pas un mécanisme de relance idempotente. L'idempotence protège les copies d'un même message portant le même identifiant.

