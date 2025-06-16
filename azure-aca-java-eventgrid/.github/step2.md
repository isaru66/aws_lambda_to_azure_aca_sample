## Implement Springboot Rest API to accept Azure Event Grid
received POST on /api/webhook

Eample of Microsoft.EventGrid.SubscriptionValidationEvent
```json
[{
  "id": "ce36d184-0061-484c-9922-d2870b671d27",
  "topic": "/subscriptions/79e1d757-ecdb-4dc3-b0b4-035bac76053d/resourceGroups/rg-isaru66-aca/providers/Microsoft.Storage/storageAccounts/stisaru66acatrigger",
  "subject": "",
  "data": {
    "validationCode": "8C79BD5E-4563-47AC-B93B-81876A844231",
    "validationUrl": "https://rp-southeastasia.eventgrid.azure.net:553/eventsubscriptions/event-grid-viewer/validate?id=8C79BD5E-4563-47AC-B93B-81876A844231&t=2025-06-16T06:08:50.2128705Z&apiVersion=2024-12-15-preview&token=h02qvC7f4E%2bcCcRIx3aRUllrbeoYlRY4U%2fo%2fcu9R9jw%3d"
  },
  "eventType": "Microsoft.EventGrid.SubscriptionValidationEvent",
  "eventTime": "2025-06-16T06:08:50.2129447Z",
  "metadataVersion": "1",
  "dataVersion": "2"
}]
```

Example of Microsoft.Storage.BlobCreated
```json
[{
  "topic": "/subscriptions/79e1d757-ecdb-4dc3-b0b4-035bac76053d/resourceGroups/rg-isaru66-aca/providers/Microsoft.Storage/storageAccounts/stisaru66acatrigger",
  "subject": "/blobServices/default/containers/blobinput/blobs/input/cat001.jpg",
  "eventType": "Microsoft.Storage.BlobCreated",
  "id": "7e82712b-901e-0003-2e85-de349b069525",
  "data": {
    "api": "PutBlob",
    "clientRequestId": "4e3fa026-ae2d-4384-9be8-0066f9ed002b",
    "requestId": "7e82712b-901e-0003-2e85-de349b000000",
    "eTag": "0x8DDAC9C792C2152",
    "contentType": "image/jpeg",
    "contentLength": 104699,
    "blobType": "BlockBlob",
    "accessTier": "Default",
    "url": "https://stisaru66acatrigger.blob.core.windows.net/blobinput/input/cat001.jpg",
    "sequencer": "0000000000000000000000000000FB71000000000000b9d1",
    "storageDiagnostics": {
      "batchId": "68f5e4a2-0006-0042-0085-de1c88000000"
    }
  },
  "dataVersion": "",
  "metadataVersion": "1",
  "eventTime": "2025-06-16T06:10:20.429935Z"
}]
```