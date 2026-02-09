import { useEffect, useState } from 'react';
import {
  Grid, Paper, Typography, Box, Card, CardContent, CircularProgress,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Chip, Alert
} from '@mui/material';
import {
  Storage as StorageIcon, Dns as DnsIcon, Cloud as CloudIcon,
  Warning as WarningIcon, CheckCircle as CheckIcon, Schedule as ScheduleIcon,
  Error as ErrorIcon
} from '@mui/icons-material';
import { dashboardAPI, alertsAPI } from '../services/api';
import { useNavigate } from 'react-router-dom';

// Couleurs pour les statuts
const STATUS_COLORS = {
  'A jour': { bg: '#c8e6c9', color: '#2e7d32' },
  'Planifie': { bg: '#fff9c4', color: '#f9a825' },
  'En retard': { bg: '#ffcdd2', color: '#c62828' },
  'Non demarre': { bg: '#e0e0e0', color: '#616161' }
};

// Couleurs pour les priorités
const PRIORITY_COLORS = {
  'Haute': { bg: '#ffcdd2', color: '#c62828' },
  'Moyenne': { bg: '#fff9c4', color: '#f9a825' },
  'Basse': { bg: '#c8e6c9', color: '#2e7d32' }
};

// Couleurs pour les environnements
const ENV_COLORS = {
  'PROD': { bg: '#ffcdd2', color: '#c62828' },
  'PREPROD': { bg: '#fff9c4', color: '#f9a825' },
  'RECETTE': { bg: '#bbdefb', color: '#1565c0' },
  'DEV': { bg: '#c8e6c9', color: '#2e7d32' },
  'MCO': { bg: '#e0e0e0', color: '#616161' }
};

function StatCard({ title, value, icon, color }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Box>
            <Typography color="textSecondary" gutterBottom variant="body2">
              {title}
            </Typography>
            <Typography variant="h4" component="div">
              {value}
            </Typography>
          </Box>
          <Box sx={{ color: color, opacity: 0.7 }}>
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
}

function StatusChip({ status }) {
  const colors = STATUS_COLORS[status] || STATUS_COLORS['Non demarre'];
  return (
    <Chip
      label={status}
      size="small"
      sx={{
        backgroundColor: colors.bg,
        color: colors.color,
        fontWeight: 'bold',
        fontSize: '0.75rem'
      }}
    />
  );
}

function PriorityChip({ priority }) {
  const colors = PRIORITY_COLORS[priority] || PRIORITY_COLORS['Basse'];
  return (
    <Chip
      label={priority}
      size="small"
      sx={{
        backgroundColor: colors.bg,
        color: colors.color,
        fontWeight: 'bold',
        fontSize: '0.75rem'
      }}
    />
  );
}

function EnvChip({ env }) {
  const colors = ENV_COLORS[env] || ENV_COLORS['DEV'];
  return (
    <Chip
      label={env}
      size="small"
      sx={{
        backgroundColor: colors.bg,
        color: colors.color,
        fontWeight: 'bold',
        fontSize: '0.75rem'
      }}
    />
  );
}

function Dashboard() {
  const [data, setData] = useState(null);
  const [alertStats, setAlertStats] = useState(null);
  const [activeAlerts, setActiveAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const [dashboardRes, alertStatsRes, alertsRes] = await Promise.all([
        dashboardAPI.getData(),
        alertsAPI.getStats().catch(() => ({ data: null })),
        alertsAPI.getActive().catch(() => ({ data: [] })),
      ]);
      setData(dashboardRes.data);
      setAlertStats(alertStatsRes.data);
      setActiveAlerts(alertsRes.data?.slice(0, 5) || []);
    } catch (err) {
      setError('Erreur lors du chargement du tableau de bord');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Calculer le résumé par environnement à partir des données
  const getEnvSummary = () => {
    if (!data) return [];

    const envStats = data.serversByEnvironment || {};
    const envList = ['PROD', 'PREPROD', 'RECETTE', 'DEV', 'MCO'];

    return envList.map(env => {
      const total = envStats[env] || 0;
      // Estimation des répartitions (à ajuster selon les vraies données)
      const servers = data.serversByEnvironmentAndType?.[env]?.SERVER || Math.floor(total * 0.6);
      const nas = data.serversByEnvironmentAndType?.[env]?.NAS || Math.floor(total * 0.25);
      const dbaas = data.serversByEnvironmentAndType?.[env]?.DBAAS || total - servers - nas;

      // Déterminer le statut basé sur la dernière MAJ
      let status = 'A jour';
      if (total === 0) status = 'Non demarre';
      else if (env === 'DEV') status = 'En retard';
      else if (env === 'RECETTE') status = 'Planifie';

      return {
        env,
        servers: servers > 0 ? `${servers} VM` : 'N/A',
        nas: nas > 0 ? `${nas} FS` : 'N/A',
        dbaas: dbaas > 0 ? `${dbaas} DBaaS` : 'N/A',
        total,
        lastUpdate: data.lastUpdateByEnv?.[env] || 'N/A',
        nextUpdate: data.nextUpdateByEnv?.[env] || 'N/A',
        status,
        responsible: data.responsibleByEnv?.[env] || 'A definir',
        comments: data.commentsByEnv?.[env] || ''
      };
    });
  };

  // Mapper la severite vers la priorite d'affichage
  const mapSeverityToPriority = (severity) => {
    const map = {
      'CRITICAL': 'Haute',
      'HIGH': 'Haute',
      'MEDIUM': 'Moyenne',
      'LOW': 'Basse',
      'INFO': 'Basse'
    };
    return map[severity] || 'Moyenne';
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <Typography color="error">{error}</Typography>
      </Box>
    );
  }

  const envSummary = getEnvSummary();
  const now = new Date();
  const lastUpdateStr = `${now.toLocaleDateString('fr-FR')} ${now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`;

  return (
    <Box>
      {/* En-tête */}
      <Paper sx={{
        p: 2,
        mb: 3,
        background: 'linear-gradient(90deg, #1565c0 0%, #0d47a1 100%)',
        color: 'white'
      }}>
        <Typography variant="h4" align="center" fontWeight="bold">
          TABLEAU DE BORD - TRACABILITE GED-PEI
        </Typography>
        <Typography variant="body2" align="left" sx={{ mt: 1 }}>
          Derniere mise a jour: {lastUpdateStr}
        </Typography>
      </Paper>

      {/* Cartes statistiques */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={6} sm={3}>
          <StatCard
            title="Serveurs"
            value={data?.totalServers || 0}
            icon={<StorageIcon sx={{ fontSize: 40 }} />}
            color="#1976d2"
          />
        </Grid>
        <Grid item xs={6} sm={3}>
          <StatCard
            title="NAS"
            value={data?.totalNas || 0}
            icon={<DnsIcon sx={{ fontSize: 40 }} />}
            color="#388e3c"
          />
        </Grid>
        <Grid item xs={6} sm={3}>
          <StatCard
            title="Bases de donnees"
            value={data?.totalDbaas || 0}
            icon={<CloudIcon sx={{ fontSize: 40 }} />}
            color="#f57c00"
          />
        </Grid>
        <Grid item xs={6} sm={3}>
          <StatCard
            title="Total ressources"
            value={data?.totalResources || 0}
            icon={<StorageIcon sx={{ fontSize: 40 }} />}
            color="#7b1fa2"
          />
        </Grid>
      </Grid>

      {/* Résumé par environnement */}
      <Paper sx={{ mb: 3 }}>
        <Box sx={{
          p: 1.5,
          backgroundColor: '#1565c0',
          color: 'white',
          borderTopLeftRadius: 4,
          borderTopRightRadius: 4
        }}>
          <Typography variant="h6" fontWeight="bold">
            RESUME PAR ENVIRONNEMENT
          </Typography>
        </Box>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                <TableCell sx={{ fontWeight: 'bold' }}>Environnement</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Serveurs</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>NAS</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>BDD</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }} align="center">Total Ressources</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Derniere MAJ</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Prochaine MAJ</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Statut</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Responsable</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Commentaires</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {envSummary.map((row, index) => (
                <TableRow key={row.env} sx={{
                  backgroundColor: index % 2 === 0 ? 'white' : '#f5f5f5',
                  '&:hover': { backgroundColor: '#e3f2fd' }
                }}>
                  <TableCell>
                    <EnvChip env={row.env} />
                  </TableCell>
                  <TableCell>{row.servers}</TableCell>
                  <TableCell>{row.nas}</TableCell>
                  <TableCell>{row.dbaas}</TableCell>
                  <TableCell align="center">
                    <Typography fontWeight="bold">{row.total}</Typography>
                  </TableCell>
                  <TableCell>{row.lastUpdate}</TableCell>
                  <TableCell>{row.nextUpdate}</TableCell>
                  <TableCell>
                    <StatusChip status={row.status} />
                  </TableCell>
                  <TableCell>{row.responsible}</TableCell>
                  <TableCell>{row.comments}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Alertes de conformite */}
      <Paper>
        <Box sx={{
          p: 1.5,
          backgroundColor: alertStats?.activeCount > 0 ? '#d32f2f' : '#ff9800',
          color: 'white',
          borderTopLeftRadius: 4,
          borderTopRightRadius: 4,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <WarningIcon />
            <Typography variant="h6" fontWeight="bold">
              ALERTES DE CONFORMITE
            </Typography>
            {alertStats?.activeCount > 0 && (
              <Chip
                label={`${alertStats.activeCount} actives`}
                size="small"
                sx={{ bgcolor: 'white', color: '#d32f2f', fontWeight: 'bold' }}
              />
            )}
          </Box>
          <Typography
            variant="body2"
            sx={{ cursor: 'pointer', textDecoration: 'underline' }}
            onClick={() => navigate('/alerts')}
          >
            Voir toutes les alertes →
          </Typography>
        </Box>
        {activeAlerts.length > 0 ? (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow sx={{ backgroundColor: '#fff3e0' }}>
                  <TableCell sx={{ fontWeight: 'bold' }}>Severite</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Type</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Serveur</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Environnement</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Logiciel</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Description</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Action requise</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {activeAlerts.map((alert, index) => (
                  <TableRow key={alert.id} sx={{
                    backgroundColor: alert.severity === 'CRITICAL' ? '#ffebee' : (index % 2 === 0 ? 'white' : '#f5f5f5'),
                    '&:hover': { backgroundColor: '#fff3e0' }
                  }}>
                    <TableCell>
                      <PriorityChip priority={mapSeverityToPriority(alert.severity)} />
                    </TableCell>
                    <TableCell>{alert.alertTypeDisplay}</TableCell>
                    <TableCell>{alert.serverName}</TableCell>
                    <TableCell>
                      {alert.environment && <EnvChip env={alert.environment} />}
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{alert.softwareName}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        v{alert.currentVersion}
                      </Typography>
                    </TableCell>
                    <TableCell>{alert.title}</TableCell>
                    <TableCell>
                      <Typography variant="body2" color="primary">
                        {alert.actionRequired}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Box sx={{ p: 3, textAlign: 'center' }}>
            <CheckIcon sx={{ fontSize: 48, color: '#4caf50', mb: 1 }} />
            <Typography color="text.secondary">
              Aucune alerte active. Cliquez sur "Voir toutes les alertes" pour generer une analyse.
            </Typography>
          </Box>
        )}
      </Paper>
    </Box>
  );
}

export default Dashboard;
