// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "configfetcher.h"
#include "configpoller.h"
#include <vespa/config/common/exceptions.h>
#include <vespa/config/common/configcontext.h>
#include <vespa/vespalib/util/thread.h>

#include <vespa/log/log.h>
LOG_SETUP(".config.helper.configfetcher");

namespace config {

VESPA_THREAD_STACK_TAG(config_fetcher_thread);

ConfigFetcher::ConfigFetcher(std::shared_ptr<IConfigContext> context)
    : _poller(std::make_unique<ConfigPoller>(std::move(context))),
      _thread(),
      _closed(false),
      _started(false)
{
}

ConfigFetcher::ConfigFetcher(const SourceSpec & spec)
    : ConfigFetcher(std::make_shared<ConfigContext>(spec))
{
}

void
ConfigFetcher::start()
{
    if (!_closed) {
        LOG(debug, "Polling for config");
        _poller->poll();
        if (_poller->getGeneration() == -1) {
            throw ConfigTimeoutException("ConfigFetcher::start timed out getting initial config");
        }
        LOG(debug, "Starting fetcher thread...");
        _thread = vespalib::thread::start(*_poller, config_fetcher_thread);
        _started = true;
        LOG(debug, "Fetcher thread started");
    }
}

ConfigFetcher::~ConfigFetcher()
{
    close();
}

int64_t
ConfigFetcher::getGeneration() const {
    return _poller->getGeneration();
}

void
ConfigFetcher::close()
{
    if (!_closed) {
        _poller->close();
        if (_started)
            _thread.join();
        _closed = true;
    }
}

} // namespace config
