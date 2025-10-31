import os
import asyncio
from collections import defaultdict
from typing import TYPE_CHECKING

from aiohttp import web

from bitraam.util import log_exceptions, ignore_exceptions
from bitraam.logging import Logger
from bitraam.util import EventListener
from bitraam.lnaddr import lndecode
from bitraam.daemon import AuthenticatedServer


if TYPE_CHECKING:
    from bitraam.network import Network


class WatchTowerServer(AuthenticatedServer):

    def __init__(self, watchtower, network: 'Network', port:int):
        self.port = port
        self.config = network.config
        self.network = network
        watchtower_user = self.config.WATCHTOWER_SERVER_USER or ""
        watchtower_password = self.config.WATCHTOWER_SERVER_PASSWORD or ""
        AuthenticatedServer.__init__(self, watchtower_user, watchtower_password)
        self.lnwatcher = watchtower
        self.app = web.Application()
        self.app.router.add_post("/", self.handle)
        self.register_method(self.get_ctn)
        self.register_method(self.add_sweep_tx)

    async def run(self):
        self.runner = web.AppRunner(self.app)
        await self.runner.setup()
        site = web.TCPSite(self.runner, host='localhost', port=self.port)
        await site.start()
        self.logger.info(f"running and listening on port {self.port}")

    async def get_ctn(self, *args):
        return await self.lnwatcher.get_ctn(*args)

    async def add_sweep_tx(self, *args):
        return await self.lnwatcher.sweepstore.add_sweep_tx(*args)

