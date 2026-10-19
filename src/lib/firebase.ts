import { initializeApp, getApps, getApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyBNTxdeuqc3F7R4eyjmwdQqQ2YWv9-9yv4",
  authDomain: "hawa-a74a6.firebaseapp.com",
  projectId: "hawa-a74a6",
  storageBucket: "hawa-a74a6.firebasestorage.app",
  messagingSenderId: "164837688319",
  appId: "1:164837688319:web:36eba5be1968fc95dd9dca",
};

const app = !getApps().length ? initializeApp(firebaseConfig) : getApp();

export const auth = getAuth(app);
export const provider = new GoogleAuthProvider();
export const googleProvider = new GoogleAuthProvider();
