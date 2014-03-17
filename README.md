# aklef/CEG3585Lab3

Ce projet vise à virtualiser le protocole de comunication HDLC utilisant une fenêtre coulissante pour les frames.

> MyServer.java est le serveur
> 
> MyClientWin.java le client.
 
Ce repo contient des programmes Java qui peuvent être compilés sur un ordinateur ayant eclipse d'installé et configuré.
 
Le serveur peut être exécuté avec un paramètre – le numéro de port:
 
    java MyServer 4444
(ou tout autre numéro de port valide)
 
Le client peut rouler sur toute autre station (ayant une JRE ou la JDK) 
à l’aide de la commande suivante:
 
    java MyClientWin

Ou en pesant "run" dans eclipse.
 
Lorsque "connecte" est appuyé, le client demandera l’adresse IP du serveur, 
le nom de la personne se connectant (comme identificateur), 
et le numéro de port (4444 dans l’exemple précédent).

Si le client n'entre rien en pesant continuellement "ok", les valeurs par défault suivantes seront utilisées:


    IP: 127.0.0.1 (loopback)
    Port: 4444 (un oeuf)
	Nom: Anonymous

