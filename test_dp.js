const fs = require('fs');

const roomSettingPath = 'components/RoomSettingPage.tsx';
let content = fs.readFileSync(roomSettingPath, 'utf8');

// Check if DP fallback needs to be `/default-avatar.png` and room name `Room`
// "The system-wide default fallbacks must always be exactly 'Room' for the room name and '/default-avatar.png' for the image across the frontend and backend."

content = content.replace(
  "useState<string>(roomData?.roomDp || '/1784533036732~2.jpg')",
  "useState<string>(roomData?.roomDp || '/default-avatar.png')"
);

content = content.replace(
  "useState<string>(roomData?.roomName || '')",
  "useState<string>(roomData?.roomName || 'Room')"
);

fs.writeFileSync(roomSettingPath, content);

console.log('Fixed fallbacks in RoomSettingPage.tsx');
