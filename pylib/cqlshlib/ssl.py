# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import sys
import ConfigParser
from thrift.transport import TSSLSocket, TTransport

def ssl_transport_factory(host, port, env, config_file):
    """
    SSL Thrift transport factory function.

    Params:
    * host .........: hostname of Cassandra node.
    * port .........: port number to connect to.
    * env ..........: environment variables. SSL factory will use, if passed,
                      SSL_CACERTS and SSL_VALIDATE variables.
    * config_file ..: path to cqlsh config file (usually ~/.cqlshrc).
                      SSL factory will use, if set, certfile and validate
                      options in [ssl] section, as well as host to certfile
                      mapping in [certfiles] section.

    [ca_certs] section is optional, 'validate' setting in [ssl] section is
    optional too. If validation is enabled then SSL certfile must be provided
    either in the config file or as an environment variable.
    Environment variables override any options set in cqlsh config file.
    """
    configs = ConfigParser.SafeConfigParser()
    configs.read(config_file)

    def get_option(section, option):
        try:
            return configs.get(section, option)
        except ConfigParser.Error:
            return None

    ssl_validate = env.get('SSL_VALIDATE')
    if ssl_validate is None:
        ssl_validate = get_option('ssl', 'validate')
    ssl_validate = ssl_validate is None or ssl_validate.lower() != 'false'

    ssl_cacerts = env.get('SSL_CACERTS')
    if ssl_cacerts is None:
        ssl_cacerts = get_option('ca_certs', host)
    if ssl_cacerts is None:
        ssl_cacerts = get_option('ssl', 'ca_certs')
    if ssl_validate and ssl_cacerts is None:
        sys.exit("Validation is enabled; SSL transport factory requires a valid certfile "
                 "to be specified. Please provide path to the certfile in [ssl] section "
                 "as 'ca_certs' option in %s (or use [ca_certs] section) or set SSL_CACERTS "
                 "environment variable." % (config_file,))
    if not ssl_cacerts is None:
        ssl_cacerts = os.path.expanduser(ssl_cacerts)

    ssl_client_cert = env.get('SSL_CLIENTCERT')
    if ssl_client_cert is None:
        ssl_client_cert = get_option('ssl', 'client_cert')
    
    tsocket = TSSLSocket.TSSLSocket(host, port, ca_certs=ssl_cacerts,
                                    validate=ssl_validate, 
                                    certfile=ssl_client_cert)
    return TTransport.TFramedTransport(tsocket)
