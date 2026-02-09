import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, Grid, Chip, Button, CircularProgress,
  Divider, Card, CardContent, IconButton, Tab, Tabs,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  FormControl, InputLabel, Select, MenuItem, Alert
} from '@mui/material';
import {
  ArrowBack as BackIcon, Edit as EditIcon, Delete as DeleteIcon,
  Computer as ServerIcon, Storage as NasIcon, DataObject as DbIcon,
  LocationOn as LocationIcon, NetworkCheck as NetworkIcon,
  Add as AddIcon, Warning as WarningIcon, CheckCircle as CheckIcon
} from '@mui/icons-material';
import { serversAPI, serverSoftwareAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

// Types de logiciels
const SOFTWARE_TYPES = [
  { value: 'RUNTIME', label: 'Runtime (Java, .NET)' },
  { value: 'MIDDLEWARE', label: 'Middleware (Tomcat, JBoss)' },
  { value: 'WEBSERVER', label: 'Serveur Web (Apache, Nginx)' },
  { value: 'DATABASE', label: 'Base de donnees' },
  { value: 'APPLICATION', label: 'Application metier' },
  { value: 'GED', label: 'GED / Documentum' },
  { value: 'MONITORING', label: 'Monitoring / Supervision' },
  { value: 'SECURITY', label: 'Securite' },
  { value: 'TOOL', label: 'Outil systeme' }
];

// Statuts de MAJ
const UPDATE_STATUSES = [
  { value: 'UP_TO_DATE', label: 'A jour', color: 'success' },
  { value: 'UPDATE_AVAILABLE', label: 'MAJ disponible', color: 'info' },
  { value: 'UPDATE_REQUIRED', label: 'MAJ requise', color: 'warning' },
  { value: 'UPDATE_PLANNED', label: 'MAJ planifiee', color: 'primary' },
  { value: 'END_OF_LIFE', label: 'Fin de vie', color: 'error' },
  { value: 'UNKNOWN', label: 'Inconnu', color: 'default' }
];

// Logiciels courants GED-PEI pour auto-completion
const COMMON_SOFTWARE = [
  // Runtime
  { name: 'OpenJDK', type: 'RUNTIME', defaultPort: null },
  { name: 'Oracle JDK', type: 'RUNTIME', defaultPort: null },
  { name: 'Java', type: 'RUNTIME', defaultPort: null },
  // Middleware
  { name: 'Tomcat', type: 'MIDDLEWARE', defaultPort: 8080 },
  { name: 'JBoss EAP', type: 'MIDDLEWARE', defaultPort: 8080 },
  { name: 'WildFly', type: 'MIDDLEWARE', defaultPort: 8080 },
  { name: 'WebLogic', type: 'MIDDLEWARE', defaultPort: 7001 },
  // Web Server
  { name: 'Apache HTTP', type: 'WEBSERVER', defaultPort: 80 },
  { name: 'Nginx', type: 'WEBSERVER', defaultPort: 80 },
  { name: 'HAProxy', type: 'WEBSERVER', defaultPort: 80 },
  // GED / Documentum
  { name: 'Documentum', type: 'GED', defaultPort: 1489 },
  { name: 'Documentum Content Server', type: 'GED', defaultPort: 1489 },
  { name: 'Documentum DA', type: 'GED', defaultPort: 8080 },
  { name: 'xCP', type: 'GED', defaultPort: 8080 },
  { name: 'D2', type: 'GED', defaultPort: 8080 },
  { name: 'OPIDIS', type: 'GED', defaultPort: null },
  { name: 'OIDIS', type: 'GED', defaultPort: null },
  // Database
  { name: 'Oracle Database', type: 'DATABASE', defaultPort: 1521 },
  { name: 'PostgreSQL', type: 'DATABASE', defaultPort: 5432 },
  { name: 'MySQL', type: 'DATABASE', defaultPort: 3306 },
  { name: 'SQL Server', type: 'DATABASE', defaultPort: 1433 },
  // Monitoring
  { name: 'Zabbix Agent', type: 'MONITORING', defaultPort: 10050 },
  { name: 'Prometheus', type: 'MONITORING', defaultPort: 9090 },
  { name: 'Grafana', type: 'MONITORING', defaultPort: 3000 },
  // Security
  { name: 'CrowdStrike', type: 'SECURITY', defaultPort: null },
  { name: 'Trend Micro', type: 'SECURITY', defaultPort: null },
  // Tools
  { name: 'Ansible', type: 'TOOL', defaultPort: null },
  { name: 'Python', type: 'TOOL', defaultPort: null },
  { name: 'Node.js', type: 'TOOL', defaultPort: null },
];

// Liste simple des noms pour le Select
const SOFTWARE_NAMES = COMMON_SOFTWARE.map(s => s.name);

function ServerDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [server, setServer] = useState(null);
  const [software, setSoftware] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tabValue, setTabValue] = useState(0);

  // Dialog pour ajouter/modifier un logiciel
  const [softwareDialog, setSoftwareDialog] = useState(false);
  const [editingSoftware, setEditingSoftware] = useState(null);
  const [softwareForm, setSoftwareForm] = useState({
    softwareName: '',
    softwareType: '',
    currentVersion: '',
    targetVersion: '',
    updateStatus: 'UNKNOWN',
    port: '',
    installPath: '',
    notes: ''
  });

  useEffect(() => {
    loadServer();
  }, [id]);

  const loadServer = async () => {
    try {
      setLoading(true);
      const [serverRes, softwareRes] = await Promise.all([
        serversAPI.getById(id),
        serverSoftwareAPI.getByServer(id)
      ]);
      setServer(serverRes.data);
      setSoftware(softwareRes.data || []);
    } catch (err) {
      console.error('Erreur chargement serveur:', err);
      setError('Serveur non trouve');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (window.confirm('Etes-vous sur de vouloir supprimer ce serveur ?')) {
      try {
        await serversAPI.delete(id);
        navigate('/servers');
      } catch (err) {
        console.error('Erreur suppression:', err);
        alert('Erreur lors de la suppression');
      }
    }
  };

  // Gestion des logiciels
  const openSoftwareDialog = (sw = null) => {
    if (sw) {
      setEditingSoftware(sw);
      setSoftwareForm({
        softwareName: sw.softwareName || '',
        softwareType: sw.softwareType || '',
        currentVersion: sw.currentVersion || '',
        targetVersion: sw.targetVersion || '',
        updateStatus: sw.updateStatus || 'UNKNOWN',
        port: sw.port || '',
        installPath: sw.installPath || '',
        notes: sw.notes || ''
      });
    } else {
      setEditingSoftware(null);
      setSoftwareForm({
        softwareName: '',
        softwareType: '',
        currentVersion: '',
        targetVersion: '',
        updateStatus: 'UNKNOWN',
        port: '',
        installPath: '',
        notes: ''
      });
    }
    setSoftwareDialog(true);
  };

  // Auto-remplissage quand on selectionne un logiciel connu
  const handleSoftwareNameChange = (value) => {
    const knownSoftware = COMMON_SOFTWARE.find(s => s.name === value);
    if (knownSoftware) {
      setSoftwareForm(prev => ({
        ...prev,
        softwareName: value,
        softwareType: prev.softwareType || knownSoftware.type,
        port: prev.port || (knownSoftware.defaultPort ? String(knownSoftware.defaultPort) : '')
      }));
    } else {
      setSoftwareForm(prev => ({ ...prev, softwareName: value }));
    }
  };

  const handleSoftwareSubmit = async () => {
    try {
      const data = {
        ...softwareForm,
        serverId: parseInt(id),
        port: softwareForm.port ? parseInt(softwareForm.port) : null
      };

      if (editingSoftware) {
        await serverSoftwareAPI.update(editingSoftware.id, data);
      } else {
        await serverSoftwareAPI.create(data);
      }

      setSoftwareDialog(false);
      // Recharger les logiciels
      const res = await serverSoftwareAPI.getByServer(id);
      setSoftware(res.data || []);
    } catch (err) {
      console.error('Erreur sauvegarde logiciel:', err);
      alert('Erreur lors de la sauvegarde');
    }
  };

  const handleDeleteSoftware = async (swId) => {
    if (window.confirm('Supprimer ce logiciel ?')) {
      try {
        await serverSoftwareAPI.delete(swId);
        const res = await serverSoftwareAPI.getByServer(id);
        setSoftware(res.data || []);
      } catch (err) {
        console.error('Erreur suppression logiciel:', err);
      }
    }
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case 'SERVER': return <ServerIcon />;
      case 'NAS': return <NasIcon />;
      case 'DBAAS': return <DbIcon />;
      default: return <ServerIcon />;
    }
  };

  const getTypeColor = (type) => {
    switch (type) {
      case 'SERVER': return 'primary';
      case 'NAS': return 'success';
      case 'DBAAS': return 'warning';
      default: return 'default';
    }
  };

  const getEnvColor = (env) => {
    switch (env?.toUpperCase()) {
      case 'PROD': return 'error';
      case 'PREPROD': return 'warning';
      case 'RECETTE': return 'info';
      case 'DEV': return 'success';
      default: return 'default';
    }
  };

  const getStatusColor = (status) => {
    const found = UPDATE_STATUSES.find(s => s.value === status);
    return found ? found.color : 'default';
  };

  const getStatusLabel = (status) => {
    const found = UPDATE_STATUSES.find(s => s.value === status);
    return found ? found.label : status;
  };

  const getTypeLabel = (type) => {
    const found = SOFTWARE_TYPES.find(t => t.value === type);
    return found ? found.label : type || 'Non defini';
  };

  // Grouper les logiciels par type
  const groupedSoftware = software.reduce((acc, sw) => {
    const type = sw.softwareType || 'OTHER';
    if (!acc[type]) acc[type] = [];
    acc[type].push(sw);
    return acc;
  }, {});

  // Compter les MAJ requises
  const updatesNeeded = software.filter(s =>
    s.updateStatus === 'UPDATE_REQUIRED' || s.updateStatus === 'END_OF_LIFE'
  ).length;

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/servers')}>
          Retour
        </Button>
        <Paper sx={{ p: 3, mt: 2, textAlign: 'center' }}>
          <Typography color="error">{error}</Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <IconButton onClick={() => navigate('/servers')}>
            <BackIcon />
          </IconButton>
          <Box display="flex" alignItems="center" gap={1}>
            {getTypeIcon(server.serverType)}
            <Typography variant="h5">
              {server.resourceName || server.hostname}
            </Typography>
          </Box>
          <Chip
            label={server.serverTypeDisplay || server.serverType}
            color={getTypeColor(server.serverType)}
            size="small"
          />
          {server.environmentName && (
            <Chip
              label={server.environmentName}
              color={getEnvColor(server.environmentName)}
              size="small"
              variant="outlined"
            />
          )}
          {server.osTypeDisplay && (
            <Chip
              label={server.osTypeDisplay}
              size="small"
              variant="outlined"
              color={server.osType === 'LINUX' ? 'warning' : 'info'}
            />
          )}
          {server.infrastructureType && (
            <Chip
              label={server.infrastructureType === 'PHYSICAL' ? 'Physique' : 'VM'}
              size="small"
              variant="outlined"
              color={server.infrastructureType === 'PHYSICAL' ? 'secondary' : 'info'}
            />
          )}
        </Box>
        {isAuthenticated() && (
          <Box display="flex" gap={1}>
            <Button
              variant="outlined"
              startIcon={<EditIcon />}
              onClick={() => navigate(`/servers/${id}/edit`)}
            >
              Modifier
            </Button>
            <Button
              variant="outlined"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={handleDelete}
            >
              Supprimer
            </Button>
          </Box>
        )}
      </Box>

      {/* Alertes MAJ */}
      {updatesNeeded > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }} icon={<WarningIcon />}>
          {updatesNeeded} logiciel(s) necessitent une mise a jour
        </Alert>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 2 }}>
        <Tabs value={tabValue} onChange={(e, v) => setTabValue(v)}>
          <Tab label="Informations generales" />
          <Tab label="Reseau" />
          <Tab label={
            <Box display="flex" alignItems="center" gap={1}>
              Logiciels installes
              {software.length > 0 && (
                <Chip label={software.length} size="small" color="primary" />
              )}
            </Box>
          } />
          {server.serverType === 'DBAAS' && <Tab label="Base de donnees" />}
        </Tabs>
      </Paper>

      {/* Tab 0: Informations generales */}
      {tabValue === 0 && (
        <Grid container spacing={3}>
          {/* Identification */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Identification</Typography>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Nom ressource</Typography>
                    <Typography variant="body1">{server.resourceName || '-'}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Hostname</Typography>
                    <Typography variant="body1">{server.hostname || '-'}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Reference DAT</Typography>
                    <Typography variant="body1">{server.refDat || '-'}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Type</Typography>
                    <Typography variant="body1">{server.serverTypeDisplay || server.serverType}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Infrastructure</Typography>
                    {server.infrastructureType ? (
                      <Chip
                        label={server.infrastructureType === 'PHYSICAL' ? 'Physique' : 'Virtuel (VM)'}
                        color={server.infrastructureType === 'PHYSICAL' ? 'secondary' : 'info'}
                        size="small"
                      />
                    ) : (
                      <Typography variant="body1">-</Typography>
                    )}
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          {/* Localisation */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  <LocationIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
                  Localisation
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Site PEI</Typography>
                    <Typography variant="body1">{server.siteCode || '-'}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Data Center</Typography>
                    <Typography variant="body1">{server.dataCenter || '-'}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Environnement</Typography>
                    {server.environmentName ? (
                      <Chip label={server.environmentName} color={getEnvColor(server.environmentName)} size="small" />
                    ) : '-'}
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          {/* Systeme */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Systeme d'exploitation</Typography>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Type OS</Typography>
                    <Typography variant="body1">
                      {server.osTypeDisplay || server.os || '-'}
                    </Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Version OS</Typography>
                    <Typography variant="body1">{server.osVersion || '-'}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">vCPU</Typography>
                    <Typography variant="body1">{server.vcpu || '-'}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">RAM (Go)</Typography>
                    <Typography variant="body1">{server.ramGb || '-'}</Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          {/* Dates */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Dates</Typography>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Derniere MAJ admin</Typography>
                    <Typography variant="body1">{server.lastAdminUpdate || '-'}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">Cree le</Typography>
                    <Typography variant="body1">
                      {server.createdAt ? new Date(server.createdAt).toLocaleDateString('fr-FR') : '-'}
                    </Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          {/* Notes */}
          {server.notes && (
            <Grid item xs={12}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>Notes</Typography>
                  <Divider sx={{ mb: 2 }} />
                  <Typography variant="body1" style={{ whiteSpace: 'pre-wrap' }}>
                    {server.notes}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          )}
        </Grid>
      )}

      {/* Tab 1: Reseau */}
      {tabValue === 1 && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              <NetworkIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
              Configuration reseau
            </Typography>
            <Divider sx={{ mb: 2 }} />
            <Grid container spacing={3}>
              <Grid item xs={12} md={4}>
                <Typography variant="body2" color="text.secondary">IP Front</Typography>
                <Typography variant="h6" fontFamily="monospace">{server.ipFront || '-'}</Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="body2" color="text.secondary">IP Admin</Typography>
                <Typography variant="h6" fontFamily="monospace">{server.ipAdmin || '-'}</Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="body2" color="text.secondary">IP ILO/IDRAC</Typography>
                <Typography variant="h6" fontFamily="monospace">{server.ipIlo || '-'}</Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="body2" color="text.secondary">VLAN Front</Typography>
                <Typography variant="body1">{server.vlanFront || '-'}</Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="body2" color="text.secondary">VLAN Admin</Typography>
                <Typography variant="body1">{server.vlanAdmin || '-'}</Typography>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Tab 2: Logiciels installes */}
      {tabValue === 2 && (
        <Card>
          <CardContent>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6">
                Logiciels installes ({software.length})
              </Typography>
              {isAuthenticated() && (
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => openSoftwareDialog()}
                >
                  Ajouter un logiciel
                </Button>
              )}
            </Box>

            {/* Resume par type */}
            {software.length > 0 && (
              <Box display="flex" gap={1} flexWrap="wrap" mb={2}>
                {Object.entries(groupedSoftware).map(([type, items]) => (
                  <Chip
                    key={type}
                    label={`${getTypeLabel(type)}: ${items.length}`}
                    size="small"
                    variant="outlined"
                    color={type === 'GED' ? 'primary' :
                           type === 'DATABASE' ? 'warning' :
                           type === 'SECURITY' ? 'error' :
                           type === 'RUNTIME' ? 'success' : 'default'}
                  />
                ))}
              </Box>
            )}

            {/* Quick-add buttons pour logiciels courants GED-PEI */}
            {isAuthenticated() && (
              <Box mb={2}>
                <Typography variant="caption" color="text.secondary" gutterBottom display="block">
                  Ajout rapide :
                </Typography>
                <Box display="flex" gap={0.5} flexWrap="wrap">
                  {[
                    { name: 'OpenJDK', type: 'RUNTIME' },
                    { name: 'Tomcat', type: 'MIDDLEWARE', port: 8080 },
                    { name: 'Documentum', type: 'GED', port: 1489 },
                    { name: 'xCP', type: 'GED', port: 8080 },
                    { name: 'D2', type: 'GED', port: 8080 },
                    { name: 'Oracle Database', type: 'DATABASE', port: 1521 },
                    { name: 'Apache HTTP', type: 'WEBSERVER', port: 80 },
                  ].filter(s => !software.some(sw => sw.softwareName === s.name)).map(preset => (
                    <Chip
                      key={preset.name}
                      label={`+ ${preset.name}`}
                      size="small"
                      variant="outlined"
                      clickable
                      onClick={() => {
                        setSoftwareForm({
                          softwareName: preset.name,
                          softwareType: preset.type,
                          currentVersion: '',
                          targetVersion: '',
                          updateStatus: 'UNKNOWN',
                          port: preset.port ? String(preset.port) : '',
                          installPath: '',
                          notes: ''
                        });
                        setEditingSoftware(null);
                        setSoftwareDialog(true);
                      }}
                      color={preset.type === 'GED' ? 'primary' :
                             preset.type === 'DATABASE' ? 'warning' :
                             preset.type === 'RUNTIME' ? 'success' : 'default'}
                    />
                  ))}
                </Box>
              </Box>
            )}

            <Divider sx={{ mb: 2 }} />

            {software.length === 0 ? (
              <Typography color="text.secondary" align="center" py={4}>
                Aucun logiciel enregistre pour ce serveur
              </Typography>
            ) : (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                      <TableCell sx={{ fontWeight: 'bold' }}>Logiciel</TableCell>
                      <TableCell sx={{ fontWeight: 'bold' }}>Type</TableCell>
                      <TableCell sx={{ fontWeight: 'bold' }}>Version actuelle</TableCell>
                      <TableCell sx={{ fontWeight: 'bold' }}>Version cible</TableCell>
                      <TableCell sx={{ fontWeight: 'bold' }}>Statut MAJ</TableCell>
                      <TableCell sx={{ fontWeight: 'bold' }}>Port</TableCell>
                      <TableCell sx={{ fontWeight: 'bold' }}>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {software.map((sw) => (
                      <TableRow key={sw.id} hover>
                        <TableCell>
                          <Typography fontWeight="bold">{sw.softwareName}</Typography>
                          {sw.installPath && (
                            <Typography variant="caption" color="text.secondary" display="block">
                              {sw.installPath}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={getTypeLabel(sw.softwareType)}
                            size="small"
                            variant="outlined"
                            color={sw.softwareType === 'GED' ? 'primary' :
                                   sw.softwareType === 'DATABASE' ? 'warning' :
                                   sw.softwareType === 'SECURITY' ? 'error' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <Chip label={sw.currentVersion || 'N/A'} size="small" variant="outlined" />
                        </TableCell>
                        <TableCell>
                          {sw.targetVersion ? (
                            <Chip label={sw.targetVersion} size="small" color="info" />
                          ) : '-'}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={getStatusLabel(sw.updateStatus)}
                            color={getStatusColor(sw.updateStatus)}
                            size="small"
                            icon={sw.updateStatus === 'UP_TO_DATE' ? <CheckIcon /> :
                                  sw.needsUpdate ? <WarningIcon /> : null}
                          />
                        </TableCell>
                        <TableCell>{sw.port || '-'}</TableCell>
                        <TableCell>
                          {isAuthenticated() && (
                            <Box>
                              <IconButton size="small" onClick={() => openSoftwareDialog(sw)}>
                                <EditIcon fontSize="small" />
                              </IconButton>
                              <IconButton size="small" color="error" onClick={() => handleDeleteSoftware(sw.id)}>
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            </Box>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </CardContent>
        </Card>
      )}

      {/* Tab 3: Base de donnees (si DBAAS) */}
      {tabValue === 3 && server.serverType === 'DBAAS' && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              <DbIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
              Informations base de donnees
            </Typography>
            <Divider sx={{ mb: 2 }} />
            <Grid container spacing={3}>
              <Grid item xs={12} md={4}>
                <Typography variant="body2" color="text.secondary">Instance</Typography>
                <Typography variant="body1">{server.dbInstance || '-'}</Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="body2" color="text.secondary">Version</Typography>
                <Typography variant="body1">{server.dbVersion || '-'}</Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="body2" color="text.secondary">Type de base</Typography>
                <Typography variant="body1">{server.dbType || '-'}</Typography>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Dialog pour ajouter/modifier un logiciel */}
      <Dialog open={softwareDialog} onClose={() => setSoftwareDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingSoftware ? 'Modifier le logiciel' : 'Ajouter un logiciel'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Nom du logiciel</InputLabel>
                <Select
                  value={SOFTWARE_NAMES.includes(softwareForm.softwareName) ? softwareForm.softwareName : '__other__'}
                  label="Nom du logiciel"
                  onChange={(e) => {
                    if (e.target.value === '__other__') {
                      setSoftwareForm({ ...softwareForm, softwareName: '' });
                    } else {
                      handleSoftwareNameChange(e.target.value);
                    }
                  }}
                >
                  {COMMON_SOFTWARE.map(sw => (
                    <MenuItem key={sw.name} value={sw.name}>
                      {sw.name}
                      <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                        ({SOFTWARE_TYPES.find(t => t.value === sw.type)?.label || sw.type})
                      </Typography>
                    </MenuItem>
                  ))}
                  <MenuItem value="__other__">Autre (saisie libre)...</MenuItem>
                </Select>
              </FormControl>
              {(!SOFTWARE_NAMES.includes(softwareForm.softwareName)) && (
                <TextField
                  fullWidth
                  label="Nom personnalise"
                  value={softwareForm.softwareName}
                  onChange={(e) => setSoftwareForm({ ...softwareForm, softwareName: e.target.value })}
                  sx={{ mt: 1 }}
                  placeholder="Saisissez le nom du logiciel"
                />
              )}
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Type</InputLabel>
                <Select
                  value={softwareForm.softwareType}
                  label="Type"
                  onChange={(e) => setSoftwareForm({ ...softwareForm, softwareType: e.target.value })}
                >
                  {SOFTWARE_TYPES.map(t => (
                    <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Version actuelle"
                value={softwareForm.currentVersion}
                onChange={(e) => setSoftwareForm({ ...softwareForm, currentVersion: e.target.value })}
                placeholder="Ex: 11.0.2, 8.5.78"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Version cible"
                value={softwareForm.targetVersion}
                onChange={(e) => setSoftwareForm({ ...softwareForm, targetVersion: e.target.value })}
                placeholder="Si MAJ prevue"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Statut MAJ</InputLabel>
                <Select
                  value={softwareForm.updateStatus}
                  label="Statut MAJ"
                  onChange={(e) => setSoftwareForm({ ...softwareForm, updateStatus: e.target.value })}
                >
                  {UPDATE_STATUSES.map(s => (
                    <MenuItem key={s.value} value={s.value}>{s.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Port"
                type="number"
                value={softwareForm.port}
                onChange={(e) => setSoftwareForm({ ...softwareForm, port: e.target.value })}
                placeholder="Ex: 8080, 443"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Chemin d'installation"
                value={softwareForm.installPath}
                onChange={(e) => setSoftwareForm({ ...softwareForm, installPath: e.target.value })}
                placeholder="Ex: /opt/tomcat"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={2}
                label="Notes"
                value={softwareForm.notes}
                onChange={(e) => setSoftwareForm({ ...softwareForm, notes: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSoftwareDialog(false)}>Annuler</Button>
          <Button
            variant="contained"
            onClick={handleSoftwareSubmit}
            disabled={!softwareForm.softwareName}
          >
            {editingSoftware ? 'Modifier' : 'Ajouter'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default ServerDetail;
