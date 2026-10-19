export function generateStableId(uid: string): string {
  if (!uid || uid === 'N/A' || uid === 'User') return '100379620';
  if (uid === 'HUSxSvQnabgU029dWYt1TUV04hd2') return '100002';
  if (uid === 'ADqW31RGBMaosOzy0HiqexKSD7h1') return '100003';
  if (/^\d{6,10}$/.test(uid)) return uid;

  // Simple string hash function to generate a stable number
  let hash = 0;
  for (let i = 0; i < uid.length; i++) {
    const char = uid.charCodeAt(i);
    hash = (hash << 5) - hash + char;
    hash = hash & hash; // Convert to 32bit integer
  }

  // Convert to positive number and ensure it's around 8 digits
  const positiveHash = Math.abs(hash);
  // Add a base offset (e.g. 10000000) so it's a fixed length 8-digit number like a traditional ID
  const stableId = (10000000 + (positiveHash % 90000000)).toString();
  return stableId;
}
