-- Rename ceremony questions from "akad" to "matrimony" (Christian wedding wording)

UPDATE intake_question SET field_key = 'matrimonyDate',  section = 'Pemberkatan',
       label = 'Tanggal Pemberkatan (Matrimony)' WHERE field_key = 'akadDate';
UPDATE intake_question SET field_key = 'matrimonyTime',  section = 'Pemberkatan',
       label = 'Waktu Pemberkatan'                WHERE field_key = 'akadTime';
UPDATE intake_question SET field_key = 'matrimonyVenue', section = 'Pemberkatan',
       label = 'Lokasi Pemberkatan'               WHERE field_key = 'akadVenue';
