var baseLayersTypes = [
    {"id": "GOOGLE_MAPS", "base_url": '', "description": "Mapbox", "server_num": 4},
    {"id": "OSM", "base_url": 'https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png', "description": "ArcGIS", "server_num": 3}
];
baseLayersTypes.$inject = [];

angular.module('mappa.module').value("baseLayersTypes", baseLayersTypes);
