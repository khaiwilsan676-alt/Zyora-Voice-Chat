const fs = require('fs');

const roomPagePath = 'components/RoomPage.tsx';
let roomPageContent = fs.readFileSync(roomPagePath, 'utf8');

// 1. Fix Enter the Room message
roomPageContent = roomPageContent.replace(
  'if (joinMessageSentRef.current || userAccountId === "guest") return;\n    joinMessageSentRef.current = true;\n    sendMessageToSocket(\'Enter the Room\', undefined, \'join\');\n  }, [userAccountId, currentUser.name, roomId]);',
  'if (joinMessageSentRef.current || userAccountId === "guest") return;\n    joinMessageSentRef.current = true;\n    sendMessageToSocket(\'Enter the Room\', undefined, \'join\');\n  }, [userAccountId, roomId]);'
);

fs.writeFileSync(roomPagePath, roomPageContent);

const serverJsPath = 'backend/server.js';
let serverJsContent = fs.readFileSync(serverJsPath, 'utf8');

// 2. Fix Mic Mode in backend
if (!serverJsContent.includes('micMode: data.micMode')) {
  serverJsContent = serverJsContent.replace(
    'theme: data.theme || "default",\n      isLocked: Boolean(data.isLocked),',
    'theme: data.theme || "default",\n      micMode: data.micMode !== undefined ? Number(data.micMode) : 15,\n      isLocked: Boolean(data.isLocked),'
  );
  fs.writeFileSync(serverJsPath, serverJsContent);
  console.log('Fixed micMode in server.js');
}
