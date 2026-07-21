FROM node:20-alpine
WORKDIR /app
COPY serverLMG.js ./
ENV LMG_PORT=8090 LMG_DATA_DIR=/data
VOLUME ["/data"]
EXPOSE 8090
CMD ["node", "serverLMG.js"]
