# IPMA stand-in (SCRUM-76). Fixtures are the SCRUM-43 files; the process does not call api.ipma.pt.
FROM node:24-alpine
WORKDIR /app
COPY server.js .
COPY fixtures ./fixtures
ENV IPMA_SIM_MODE=ok
ENV PORT=8090
EXPOSE 8090
USER node
CMD ["node", "server.js"]
