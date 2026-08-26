import React, { useState, useEffect, useMemo } from 'react';
import {
  Box, Paper, Typography, Chip, Alert, CircularProgress, Card, CardContent,
  Grid, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, MenuItem, Button, Tooltip
} from '@mui/material';
import {
  Refresh as RefreshIcon, ArrowForward as ArrowIcon,
  ErrorOutline as ErrorIcon, CheckCircleOutline as OkIcon
} from '@mui/icons-material';
import { complianceAPI } from '../services/api';

// Ordre de gravite : il pilote l'affichage des tuiles comme le tri du tableau.
const VERDICTS = [
  { key: 'CRITIQUE', label: 'Critique', color: '#b71c1c', help: 'Version interdite pour raison cyber' },
  { key: 'A_CORRIGER', label: 'A corriger', color: '#e53935', help: 'Version interdite, ou support termine' },
  { key: 'A_SURVEILLER', label: 'A surveiller', color: '#fb8c00', help: 'Version toleree ou en trajectoire, support encore valide' },
  { key: 'CONFORME', label: 'Conforme', color: '#43a047', help: 'Version preconisee par le CSR' },
  { key: 'VERSION_INCONNUE', label: 'Version hors referentiel', color: '#757575', help: 'Produit au catalogue, mais cette version n y figure pas' },
  { key: 'HORS_CSR', label: 'Produit hors CSR', color: '#9e9e9e', help: 'Produit absent du catalogue Sipedia' },
];

const verdictOf = (key) => VERDICTS.find((v) => v.key === key) || VERDICTS[VERDICTS.length - 1];

const formatDate = (value) => {
  if (!value) return '-';
  const [year, month, day] = value.split('-');
  return `${day}/${month}/${year}`;
};

export default function Compliance() {
  const [rows, setRows] = useState([]);
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [verdictFilter, setVerdictFilter] = useState('');
  const [environmentFilter, setEnvironmentFilter] = useState('');
  const [serverFilter, setServerFilter] = useState('');

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const [rowsResponse, statsResponse] = await Promise.all([
        complianceAPI.getAll(),
        complianceAPI.getStats(),
      ]);
      setRows(rowsResponse.data);
      setStats(statsResponse.data);
    } catch (e) {
      setError("Impossible de charger le rapprochement CSR");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const environments = useMemo(
    () => [...new Set(rows.map((r) => r.environment).filter(Boolean))].sort(),
    [rows]
  );
  const servers = useMemo(
    () => [...new Set(rows.map((r) => r.serverName).filter(Boolean))].sort(),
    [rows]
  );

  const filtered = useMemo(() => rows.filter((r) =>
    (!verdictFilter || r.verdict === verdictFilter)
    && (!environmentFilter || r.environment === environmentFilter)
    && (!serverFilter || r.serverName === serverFilter)
  ), [rows, verdictFilter, environmentFilter, serverFilter]);

  const total = rows.length;
  const toFix = (stats.CRITIQUE || 0) + (stats.A_CORRIGER || 0);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" p={6}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h5">Conformite CSR</Typography>
          <Typography variant="body2" color="text.secondary">
            Ecart entre les versions relevees par Prevobs et les cibles du referentiel Sipedia
          </Typography>
        </Box>
        <Button startIcon={<RefreshIcon />} onClick={load} variant="outlined">
          Actualiser
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {total === 0 && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Aucun composant a rapprocher. Importez d'abord une extraction Prevobs
          (Obsolescence Serveur) et le referentiel CSR (Versions Logiciels).
        </Alert>
      )}

      {total > 0 && (
        <>
          <Alert
            severity={toFix > 0 ? 'warning' : 'success'}
            icon={toFix > 0 ? <ErrorIcon /> : <OkIcon />}
            sx={{ mb: 3 }}
          >
            {toFix > 0
              ? `${toFix} composant(s) sur ${total} demandent une action : version interdite ou support termine.`
              : `Les ${total} composants releves sont dans un etat acceptable.`}
          </Alert>

          <Grid container spacing={2} sx={{ mb: 3 }}>
            {VERDICTS.map((verdict) => (
              <Grid item xs={6} sm={4} md={2} key={verdict.key}>
                <Tooltip title={verdict.help}>
                  <Card
                    onClick={() => setVerdictFilter(verdictFilter === verdict.key ? '' : verdict.key)}
                    sx={{
                      cursor: 'pointer',
                      borderTop: `4px solid ${verdict.color}`,
                      bgcolor: verdictFilter === verdict.key ? 'action.selected' : 'background.paper',
                    }}
                  >
                    <CardContent sx={{ textAlign: 'center', py: 2 }}>
                      <Typography variant="h4">{stats[verdict.key] || 0}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {verdict.label}
                      </Typography>
                    </CardContent>
                  </Card>
                </Tooltip>
              </Grid>
            ))}
          </Grid>

          <Paper sx={{ p: 2, mb: 2 }}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={4}>
                <TextField
                  select fullWidth size="small" label="Environnement"
                  value={environmentFilter} onChange={(e) => setEnvironmentFilter(e.target.value)}
                >
                  <MenuItem value="">Tous</MenuItem>
                  {environments.map((e) => <MenuItem key={e} value={e}>{e}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  select fullWidth size="small" label="Serveur"
                  value={serverFilter} onChange={(e) => setServerFilter(e.target.value)}
                >
                  <MenuItem value="">Tous</MenuItem>
                  {servers.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  select fullWidth size="small" label="Verdict"
                  value={verdictFilter} onChange={(e) => setVerdictFilter(e.target.value)}
                >
                  <MenuItem value="">Tous</MenuItem>
                  {VERDICTS.map((v) => <MenuItem key={v.key} value={v.key}>{v.label}</MenuItem>)}
                </TextField>
              </Grid>
            </Grid>
          </Paper>

          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Serveur</TableCell>
                  <TableCell>Env.</TableCell>
                  <TableCell>Logiciel</TableCell>
                  <TableCell>Version installee</TableCell>
                  <TableCell>Statut CSR</TableCell>
                  <TableCell>Fin de support</TableCell>
                  <TableCell>Cible preconisee</TableCell>
                  <TableCell>Verdict</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtered.map((row, index) => {
                  const verdict = verdictOf(row.verdict);
                  return (
                    <TableRow key={`${row.serverName}-${row.softwareName}-${index}`} hover>
                      <TableCell sx={{ fontFamily: 'monospace' }}>{row.serverName}</TableCell>
                      <TableCell>{row.environment || '-'}</TableCell>
                      <TableCell>{row.softwareName}</TableCell>
                      <TableCell>
                        <Chip size="small" label={row.installedVersion || '-'} />
                        {row.csrVersion && row.csrVersion !== row.installedVersion && (
                          <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                            (CSR {row.csrVersion})
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>{row.csrStatus || '-'}</TableCell>
                      <TableCell sx={{ color: row.supportExpired ? 'error.main' : 'inherit' }}>
                        {formatDate(row.supportEndDate)}
                        {row.supportExpired && ' (depasse)'}
                      </TableCell>
                      <TableCell>
                        {row.upgradeNeeded && row.targetVersion ? (
                          <Box display="flex" alignItems="center" gap={0.5}>
                            <ArrowIcon fontSize="small" color="action" />
                            <Chip size="small" color="primary" variant="outlined" label={row.targetVersion} />
                          </Box>
                        ) : (
                          <Typography variant="caption" color="text.secondary">
                            {row.targetVersion || 'aucune cible au CSR'}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={verdict.label}
                          sx={{ bgcolor: verdict.color, color: 'white' }}
                        />
                      </TableCell>
                    </TableRow>
                  );
                })}
                {filtered.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={8} align="center" sx={{ py: 3 }}>
                      <Typography color="text.secondary">Aucune ligne pour ces filtres</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>

          <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
            {filtered.length} ligne(s) affichee(s) sur {total}. Le rapprochement se fait sur le nom
            du produit puis sur la version de roadmap : une version relevee 10.1.48 correspond a la
            version 10.1 du referentiel.
          </Typography>
        </>
      )}
    </Box>
  );
}
