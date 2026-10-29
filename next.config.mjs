/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },

  images: {
    unoptimized: true,
  },

  experimental: {
    optimizePackageImports: ['lucide-react'],
  },

  poweredByHeader: false,
};

export default nextConfig
