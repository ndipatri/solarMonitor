This project is meant to provide a platform for experimenting with resource discovery using Bluetooth Beacons.

I've instrumented my rooftop solar panels with a Bluetooth Beacon which uniquely identifies them with the vendor (SolarEdge)

This Android application scans for this beacon, retrieves unique account information, and then go to the vendor's cloud to retrieve my latest Solar Output in kilo-watt-hours.

Key points:

1. This app uses my Google-Proximity library which encapsulates Eddystone beacon scanning AND all 'Google Proximity API' work which retrieves meta-data stored in the Google cloud for a particular Beacon (e.g. my SolarEdge account id).

2. This app also retrieves solar output information from the SolarEdge website.

3. To scan for a nearby beacon, no special authentication is needed.  However, if you try to change the beacon account information using this app, an account on the phone must have permission to do this up in the cloud (see details related to "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry".

4. To retrieve solar production from the SolarEdge endpoint, you will need an APIKEY.  You must provide this as an 'solarEdgeApiKey' property in a 'gradle.properties' file.

5. This application scans for Eddystone beacons that have been configured with a specific namespaceId.  You must provide this as an 'beaconNamespaceId' property in a 'gradle.properties' file.

 


