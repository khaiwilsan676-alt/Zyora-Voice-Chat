import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Zyora Owner Panel",
  description: "Zyora administration panel",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
