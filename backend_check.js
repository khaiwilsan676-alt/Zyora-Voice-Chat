const fs = require('fs');
const path = require('path');
const serverContent = fs.readFileSync('backend/server.js', 'utf8');

if (!serverContent.includes('data.micMode')) {
    console.log('micMode is missing in server.js PUT /api/rooms');
} else {
    console.log('micMode is found');
}
